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

import com.jitlogic.zorka.common.tracedata.MetricTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JMX query object. It represents JMX query as issued by zabbix.discovery(), zorka.ls()
 * and other components. It also defines simple DSL for querying. Query objects are immutable
 * and persistent - modifier functions implemented by this class do not change object itself
 * but creates (changed) copy instead.
 *
 * JMX query in zorka defines object name, list of attributes from object name to retain and
 * rules for traversing object graph referenced from MBean (and storing selected attributes).
 * Those rules are called query segments.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class QueryDef {

    /** Empty query segment list. Used as initial value. */
    private static final List<QuerySegment> EMPTY_SEG
                = Collections.unmodifiableList(new ArrayList<QuerySegment>(1));

    public static final int NO_NULL_VALS = 0x01;

    public static final int NO_NULL_ATTRS = 0x02;

    /** Flags altering query execution. */
    private int flags;

    private String name = "<unnamed>";

    /** MBean Server name */
    private String mbsName;

    /** JMX Object name query (mask) */
    private String query;

    /** List of object name attributes to fetch */
    private List<String> attributes;

    /** List of query segments (describing traversal into subsequent object attributes) */
    private List<QuerySegment> segments;

    /** Template for generated metrics (used by some clients) */
    private MetricTemplate metricTemplate;

    /**
     * Creates new query
     *
     * @param mbsName mbean server name
     *
     * @param query JMX Object Name query
     *
     * @param attrs list of object name attributes to fetch
     */
    public QueryDef(String mbsName, String query, String... attrs) {
        this.mbsName = mbsName;
        this.query = query;
        this.attributes = Collections.unmodifiableList(Arrays.asList(attrs));
        this.segments = EMPTY_SEG;
    }


    /**
     * Creates deep copy of another query object
     *
     * @param orig query object to be copied
     */
    private QueryDef(QueryDef orig) {
        this.mbsName = orig.mbsName;
        this.query = orig.query;
        this.attributes = orig.attributes;
        this.segments = orig.segments;
        this.metricTemplate = orig.metricTemplate;
    }


    /**
     * Returns query object with additional segments.
     *
     * @param segs list of segments to add
     *
     * @return adds new segments to a query
     */
    private QueryDef withSegs(QuerySegment...segs) {
        QueryDef qdef = new QueryDef(this);

        List<QuerySegment> newSegs = new ArrayList<QuerySegment>(segments.size() + segs.length + 1);
        newSegs.addAll(segments);
        newSegs.addAll(Arrays.asList(segs));
        qdef.segments = Collections.unmodifiableList(newSegs);

        return qdef;
    }



    /**
     * Fetches attribute defined by arg parameter.
     *
     *
     * @param args attributes (as for ObjectInspector.get() method)
     *
     * @return augmented query
     */
    public QueryDef get(Object...args) {
        QueryDef qdef = this;

        for (Object arg : args) {
            qdef = qdef.withSegs(new QuerySegment(arg));
        }

        return qdef;
    }


    /**
     * Fetches attribute defined by arg parameter and stores it as
     * query result
     *
     * @param arg attribute (as for ObjectInspector.get() method)
     *
     * @param name label used to attach obtained attribute in query result.
     *
     * @return augmented query definition
     */
    public QueryDef getAs(Object arg, String name) {
        return withSegs(new QuerySegment(arg, name));
    }


    /**
     * Lists attributes and selects only those matching supplied regular expression/mask.
     *
     * @param regex regular expression or mask
     *
     * @return augmented query definition
     */
    public QueryDef list(String regex) {
        Pattern pattern = regex.startsWith("~") ? Pattern.compile(regex.substring(1))
                : Pattern.compile("^"+regex.replace("**", ".+").replace("*", "[^\\.]+")+"$");

        return withSegs(new QuerySegment(pattern));
    }


    /**
     * Lists attributes and selects only those matching supplied regular expression/mask.
     *
     * @param regex regular expression or mask
     *
     * @param name label used to attach obtained attribute to query result
     *
     * @return augmented query definition
     */
    public QueryDef listAs(String regex, String name) {
        Pattern pattern = regex.startsWith("~") ? Pattern.compile(regex.substring(1))
                : Pattern.compile("^"+regex.replace("**", ".+").replace("*", "[^\\.]+")+"$");

        return withSegs(new QuerySegment(pattern, name));
    }


    /**
     * Attaches metric template to query def. Metric templates are used by JMX attribute scanners
     * to present generated data with metrics.
     *
     * @param metricTemplate metric template object
     *
     * @return augmented query definition
     */
    public QueryDef metric(MetricTemplate metricTemplate) {
        QueryDef qdef = new QueryDef(this);
        qdef.metricTemplate = metricTemplate;
        return qdef;
    }


    public QueryDef withName(String name) {
        QueryDef qdef = new QueryDef(this);
        qdef.name = name;
        return qdef;
    }


    /**
     * Sets additional flags for query.
     *
     * @param flags additional flags to be set
     *
     * @return augmented query definition
     */
    public QueryDef with(int...flags) {
        QueryDef qdef = new QueryDef(this);

        for (int flag : flags) {
            qdef.flags |= flag;
        }

        return qdef;
    }


    public boolean hasFlags(int flags) {
        return 0 != (this.flags & flags);
    }

    public String getName() { return name; }

    public String getMbsName() {
        return mbsName;
    }

    public String getQuery() {
        return query;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public List<QuerySegment> getSegments() {
        return segments;
    }

    public MetricTemplate getMetricTemplate() {
        return metricTemplate;
    }
}
