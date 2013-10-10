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
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.fressian.FressianReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
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

    public void searchRecords(TraceRecord tr, String path, TraceDetailSearchExpression expr,
                              TraceRecordSearchResult result, long traceTime, boolean recur) {

        boolean matches = (expr.emptyExpr() || matches(tr, expr))
                && (0 == (expr.getFlags() & TraceDetailSearchExpression.ERRORS_ONLY) || null != tr.findException())
                && (0 == (expr.getFlags() & TraceDetailSearchExpression.METHODS_WITH_ATTRS) || tr.numAttrs() > 0);

        if (matches) {
            result.getResult().add(packTraceRecord(tr, path, 250));

            double pct = 100.0 * tr.getTime() / traceTime;

            result.setSumPct(result.getSumPct() + pct);
            result.setSumTime(result.getSumTime() + tr.getTime());
            result.setMaxTime(Math.max(result.getMaxTime(), tr.getTime()));
            result.setMinTime(Math.min(result.getMinTime(), tr.getTime()));

            if (!recur) {
                result.setRecurPct(result.getRecurPct() + pct);
                result.setRecurTime(result.getRecurTime() + tr.getTime());
            }
        }

        for (int i = 0; i < tr.numChildren(); i++) {
            searchRecords(tr.getChild(i), path + "/" + i, expr, result, traceTime, matches || recur);
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


    private void makeHistogram(Map<String, MethodRankInfo> histogram, TraceRecord tr) {
        String id = "" + tr.getClassId() + "." + tr.getMethodId() + "." + tr.getSignatureId();

        MethodRankInfo mri = histogram.get(id);
        if (mri == null) {
            mri = new MethodRankInfo();
            mri.setMethod(ZicoUtil.prettyPrint(tr, symbolRegistry));
            mri.setMinTime(Long.MAX_VALUE);
            mri.setMaxTime(Long.MIN_VALUE);
            mri.setMinBareTime(Long.MAX_VALUE);
            mri.setMaxBareTime(Long.MIN_VALUE);
            histogram.put(id, mri);
        }

        mri.setCalls(mri.getCalls() + 1);
        if (tr.getException() != null || tr.hasFlag(TraceRecord.EXCEPTION_PASS | TraceRecord.EXCEPTION_WRAP)) {
            mri.setErrors(mri.getErrors() + 1);
        }
        mri.setTime(mri.getTime() + tr.getTime());
        mri.setMinTime(Math.min(mri.getMinTime(), tr.getTime()));
        mri.setMaxTime(Math.max(mri.getMaxTime(), tr.getTime()));

        long bareTime = tr.getTime();

        if (tr.numChildren() > 0) {
            for (TraceRecord c : tr.getChildren()) {
                makeHistogram(histogram, c);
                bareTime -= c.getTime();
            }
        }

        mri.setBareTime(mri.getBareTime() + bareTime);
        mri.setMinBareTime(Math.min(mri.getMinBareTime(), bareTime));
        mri.setMaxBareTime(Math.max(mri.getMaxBareTime(), bareTime));
    }


    private static final Map<String, Comparator<MethodRankInfo>> RANK_COMPARATORS = ZorkaUtil.map(
            "calls.DESC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o2.getCalls() - o1.getCalls();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "calls.ASC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o1.getCalls() - o2.getCalls();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "errors.DESC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o2.getErrors() - o1.getErrors();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "errors.ASC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o1.getErrors() - o2.getErrors();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "time.DESC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o2.getTime() - o1.getTime();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "time.ASC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o1.getTime() - o2.getTime();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "avgTime.DESC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o2.getAvgTime() - o1.getAvgTime();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            },
            "avgTime.ASC",
            new Comparator<MethodRankInfo>() {
                @Override
                public int compare(MethodRankInfo o1, MethodRankInfo o2) {
                    long l = o1.getAvgTime() - o2.getAvgTime();
                    return l == 0 ? 0 : (l > 0 ? 1 : -1);
                }
            }
    );

    public List<MethodRankInfo> methodRank(String orderBy, String orderDesc) {
        TraceRecord tr = getTraceRecord("", 0);

        Map<String, MethodRankInfo> histogram = new HashMap<String, MethodRankInfo>();

        if (tr != null) {
            makeHistogram(histogram, tr);
        }

        List<MethodRankInfo> result = new ArrayList<MethodRankInfo>(histogram.size());
        result.addAll(histogram.values());

        String key = orderBy + "." + orderDesc;
        Comparator<MethodRankInfo> comparator = RANK_COMPARATORS.get(key);

        if (comparator != null) {
            Collections.sort(result, comparator);
        }

        return result;
    }
}
