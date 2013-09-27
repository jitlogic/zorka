/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.core;


import com.jitlogic.zico.data.*;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.*;
import org.fressian.FressianReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.jitlogic.zico.data.TraceDetailSearchExpression.*;

public class TraceRecordStore {

    private HostStore hostStore;
    private TraceInfo traceInfo;

    private TraceCache cache;
    private SymbolRegistry symbolRegistry;


    public TraceRecordStore(HostStore hostStore, TraceInfo traceInfo, TraceCache cache, SymbolRegistry symbolRegistry) {
        this.hostStore = hostStore;
        this.traceInfo = traceInfo;

        this.cache = cache;
        this.symbolRegistry = symbolRegistry;
    }


    private final static Pattern RE_SLASH = Pattern.compile("/");


    private boolean matches(String s, String expr) {
        return s != null && s.contains(expr);
    }


    private boolean matches(TraceRecord tr, TraceDetailSearchExpression expr) {
        if ((expr.hasFlag(SEARCH_CLASSES)
                && matches(symbolRegistry.symbolName(tr.getClassId()), expr.getSearchExpr()))
                || (expr.hasFlag(SEARCH_METHODS)
                && matches(symbolRegistry.symbolName(tr.getMethodId()), expr.getSearchExpr()))) {
            return true;
        }

        if (expr.hasFlag(SEARCH_ATTRS) && tr.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                if (matches(symbolRegistry.symbolName(e.getKey()), expr.getSearchExpr())) {
                    return true;
                }
                if (e.getValue() != null && matches(e.getValue().toString(), expr.getSearchExpr())) {
                    return true;
                }
            }
        }

        SymbolicException se = tr.findException();

        if (expr.hasFlag(SEARCH_EX_MSG) && se != null &&
                (matches(se.getMessage(), expr.getSearchExpr())
                        || matches(symbolRegistry.symbolName(se.getClassId()), expr.getSearchExpr()))) {
            return true;
        }

        if (expr.hasFlag(SEARCH_EX_STACK) && se != null) {
            for (SymbolicStackElement sse : se.getStackTrace()) {
                if (matches(symbolRegistry.symbolName(sse.getClassId()), expr.getSearchExpr())
                        || matches(symbolRegistry.symbolName(sse.getMethodId()), expr.getSearchExpr())
                        || matches(symbolRegistry.symbolName(sse.getFileId()), expr.getSearchExpr())) {
                    return true;
                }
            }
        }

        return false;
    }

    public void searchRecords(TraceRecord tr, String path,
                              TraceDetailSearchExpression expr, List<TraceRecordInfo> result) {

        boolean matches = (expr.emptyExpr() || matches(tr, expr))
                && (0 == (expr.getFlags() & TraceDetailSearchExpression.ERRORS_ONLY) || null != tr.findException())
                && (0 == (expr.getFlags() & TraceDetailSearchExpression.METHODS_WITH_ATTRS) || tr.numAttrs() > 0);

        if (matches) {
            result.add(packTraceRecord(tr, path, 250));
        }

        for (int i = 0; i < tr.numChildren(); i++) {
            searchRecords(tr.getChild(i), path + "/" + i, expr, result);
        }
    }


    public TraceRecord getTraceRecord(String path, long minMethodTime) {
        try {
            TraceRecord tr = fetchRecord(minMethodTime);
            if (path != null && path.trim().length() > 0) {
                for (String p : RE_SLASH.split(path.trim())) {
                    Integer idx = Integer.parseInt(p);
                    if (idx >= 0 && idx < tr.numChildren()) {
                        tr = tr.getChild(idx);
                    } else {
                        throw new RuntimeException("Child record of path " + path + " not found.");
                    }
                }
            }

            return tr;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving trace record.", e);
        }
    }


    private TraceRecord fetchRecord(long minMethodTime) throws IOException {
        TraceDetailFilterExpression filter = new TraceDetailFilterExpression();
        filter.setHostId(hostStore.getHostInfo().getId());
        filter.setTraceOffs(traceInfo.getDataOffs());
        filter.setMinMethodTime(minMethodTime);

        TraceRecord tr = cache.get(filter);

        if (tr == null) {
            RDSStore rds = hostStore.getRds();
            byte[] blob = rds.read(traceInfo.getDataOffs(), traceInfo.getDataLen());
            ByteArrayInputStream is = new ByteArrayInputStream(blob);
            FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            tr = (TraceRecord) reader.readObject();
            if (minMethodTime > 0) {
                tr = filterByTime(tr, minMethodTime);
            }
            cache.put(filter, tr);
        }

        return tr;
    }


    public TraceRecord filterByTime(TraceRecord orig, long minMethodTime) {
        TraceRecord tr = orig.copy();
        if (orig.getChildren() != null) {
            ArrayList<TraceRecord> children = new ArrayList<TraceRecord>(orig.numChildren());
            for (TraceRecord child : orig.getChildren()) {
                if (child.getTime() >= minMethodTime) {
                    children.add(filterByTime(child, minMethodTime));
                }
            }
            tr.setChildren(children);
        }

        return tr;
    }


    public TraceRecordInfo packTraceRecord(TraceRecord tr, String path, Integer attrLimit) {
        TraceRecordInfo info = new TraceRecordInfo();

        info.setCalls(tr.getCalls());
        info.setErrors(tr.getErrors());
        info.setTime(tr.getTime());
        info.setFlags(tr.getFlags());
        info.setMethod(ZicoUtil.prettyPrint(tr, symbolRegistry));
        info.setChildren(tr.numChildren());
        info.setPath(path);

        if (tr.getAttrs() != null) {
            Map<String, String> nattr = new HashMap<String, String>();
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                String s = "" + e.getValue();
                if (attrLimit != null && s.length() > attrLimit) {
                    s = s.substring(0, attrLimit) + "...";
                }
                nattr.put(symbolRegistry.symbolName(e.getKey()), s);
            }
            info.setAttributes(nattr);
        }

        SymbolicException sex = tr.findException();
        if (sex != null) {
            SymbolicExceptionInfo sei = ZicoUtil.extractSymbolicExceptionInfo(symbolRegistry, sex);
            info.setExceptionInfo(sei);
        }

        return info;
    }


}
