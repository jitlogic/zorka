/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.util.JmxObject;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.tracedata.MetricTemplate;

import javax.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class QueryLister {

    private static final ZorkaLog log = ZorkaLogger.getLog(QueryLister.class);

    private MBeanServerRegistry registry;
    private QueryDef query;

    public QueryLister(MBeanServerRegistry registry, QueryDef query) {
        this.registry = registry;
        this.query = query;
    }


    public List<QueryResult> list() {

        MBeanServerConnection conn = registry.lookup(query.getMbsName());

        if (conn == null) {
            log.warn(ZorkaLogger.ZAG_WARNINGS, "Trying to query non-existent MBS: " + query.getMbsName());
            return new ArrayList<QueryResult>(1);
        }

        ClassLoader cl0 = Thread.currentThread().getContextClassLoader();
        ClassLoader cl1 = registry.getClassLoader(query.getMbsName());

        if (cl1 != null) {
            Thread.currentThread().setContextClassLoader(cl1);
        }

        List<QueryResult> results;

        try {

            results = getResults(conn);

            List<QuerySegment> segments = query.getSegments();
            if (segments.size() > 1) {
                for (int i = 1; i < segments.size(); i++) {
                    QuerySegment seg = segments.get(i);
                    if (seg.getAttr() instanceof Pattern) {
                        results = expandResults(results, seg);
                    } else {
                        results = refineResults(results, seg);
                    }
                }
            }

        } finally {
            if (cl1 != null) {
                Thread.currentThread().setContextClassLoader(cl0);
            }
        }

        List<QueryResult> rslt = new ArrayList<QueryResult>(results.size());

        for (QueryResult result : results) {
            if (!query.hasFlags(QueryDef.NO_NULL_VALS) || result.getValue() != null) {
                rslt.add(result);
            }
        }

        return rslt;
    }


    private List<QueryResult> getResults(MBeanServerConnection conn) {
        Set<ObjectName> objNames = ObjectInspector.queryNames(conn, query.getQuery());
        QuerySegment seg = query.getSegments().size() > 0 ? query.getSegments().get(0) : null;

        List<QueryResult> results = new ArrayList(objNames.size() + 1);

        for (ObjectName on : objNames) {

            if (query.hasFlags(QueryDef.NO_NULL_ATTRS) && on.getKeyPropertyList().size() > query.getAttributes().size()) {
                continue;
            }

            try {
                if (seg != null && seg.getAttr() instanceof Pattern) {
                    getMultiResult(conn, seg, results, on);
                } else {
                    getSingleResult(conn, seg, results, on);
                }
            } catch (Exception e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error listing results of " + query, e);
            }
        }

        return results;

    }


    private void getMultiResult(MBeanServerConnection conn, QuerySegment seg, List<QueryResult> results, ObjectName on) {
        Pattern pattern = (Pattern) seg.getAttr();
        try {
            for (MBeanAttributeInfo attr : conn.getMBeanInfo(on).getAttributes()) {
                if (pattern.matcher(attr.getName()).matches()) {
                    makeResult(seg, results, on, conn.getAttribute(on, attr.getName()), attr.getName());
                }
            }
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error listing attributes of: " + on, e);
        }
    }


    private void getSingleResult(MBeanServerConnection conn, QuerySegment seg, List<QueryResult> results, ObjectName on) {
        try {
            makeResult(seg, results, on, seg != null
                    ? conn.getAttribute(on, seg.getAttr().toString())
                    : new JmxObject(on, conn, null),
                    seg != null ? seg.getAttr() : null);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error listing results of " + query, e);
        }
    }


    private void makeResult(QuerySegment seg, List<QueryResult> results, ObjectName on, Object val, Object attrName) {
        QueryResult rslt = new QueryResult(val);
        for (String attr : query.getAttributes()) {
            if ("*".equals(attr)) {
                rslt.setAttr("objectName", on.toString());
            } else {
                rslt.setAttr(attr, on.getKeyProperty(attr));
            }
        }

        if (attrName != null && seg != null && seg.getName() != null) {
            rslt.setAttr(seg.getName(), attrName);
        }
        results.add(rslt);
    }


    /**
     * Executes getter query segment.
     *
     * @param input
     * @param seg
     * @return
     */
    private List<QueryResult> refineResults(List<QueryResult> input, QuerySegment seg) {
        for (QueryResult res : input) {
            res.setValue(ObjectInspector.get(res.getValue(), seg.getAttr()));
            if (seg.getName() != null) {
                res.setAttr(seg.getName(), seg.getAttr()); // This attribute fetch is really trivial ...
            }
        }

        return input;
    }


    public List<QueryResult> expandResults(List<QueryResult> input, QuerySegment seg) {
        List<QueryResult> results = new ArrayList<QueryResult>(input.size() * 2 + 1);

        for (QueryResult res : input) {
            for (Object attr : ObjectInspector.list(res.getValue())) {
                if (seg.matches(attr)) {
                    QueryResult result = new QueryResult(res, ObjectInspector.get(res.getValue(), attr));
                    if (seg.getName() != null) {
                        result.setAttr(seg.getName(), attr);
                    }
                    results.add(result);
                }
            }
        }

        return results;
    }

    public MetricTemplate getMetricTemplate() {
        return query.getMetricTemplate();
    }
}
