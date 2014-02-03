/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zico.core.model.MethodRankInfo;
import com.jitlogic.zico.core.model.TraceInfoRecord;
import com.jitlogic.zico.core.model.TraceRecordSearchQuery;
import com.jitlogic.zico.core.model.TraceRecordSearchResult;
import com.jitlogic.zico.core.rds.RDSCleanupListener;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zico.core.search.TraceRecordMatcher;
import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.fressian.FressianReader;
import org.fressian.FressianWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TraceRecordStore implements Closeable {

    private final static Pattern RE_SLASH = Pattern.compile("/");

    private ZicoConfig config;

    private HostStore hostStore;
    private SymbolRegistry symbolRegistry;

    private RDSStore rds;

    public TraceRecordStore(ZicoConfig config, HostStore hostStore, String path, int fileSizeDivider,
                            RDSCleanupListener...listeners) throws IOException {
        this.config = config;
        this.hostStore = hostStore;
        this.symbolRegistry = hostStore.getSymbolRegistry();


        String rdspath = ZorkaUtil.path(hostStore.getRootPath(), path);
        File rdsDir = new File(rdspath);
        if (!rdsDir.exists()) {
            rdsDir.mkdirs();
        }

        long fileSize = config.kiloCfg("rds.file.size", 16 * 1024 * 1024L).intValue() / fileSizeDivider;

        long segmentSize = config.kiloCfg("rds.seg.size", 1024 * 1024L);

        rds = new RDSStore(rdspath,
                hostStore.getMaxSize(),
                fileSize, segmentSize,
                listeners);
    }

    public ChunkInfo write(TraceRecord tr) throws IOException {
        byte[] data = serialize(tr);
        long offs = rds.write(data);
        return new ChunkInfo(offs, data.length);
    }

    public TraceRecord read(ChunkInfo chunk) throws IOException {
        byte[] blob = rds.read(chunk.getOffset(), chunk.getLength());

        if (blob != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(blob);
            FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            return (TraceRecord) reader.readObject();
        }

        return null;
    }

    private byte[] serialize(TraceRecord rec) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(rec);
        return os.toByteArray();
    }

    public RDSStore getRds() {
        return rds;
    }

    // TODO factor out record packing from here
    public void searchRecords(TraceRecord tr, String path, TraceRecordMatcher matcher,
                              TraceRecordSearchResult result, long traceTime, boolean recur) {

        boolean matches = matcher.match(tr)
                && (!matcher.hasFlag(TraceRecordSearchQuery.ERRORS_ONLY) || null != tr.findException())
                && (!matcher.hasFlag(TraceRecordSearchQuery.METHODS_WITH_ATTRS) || tr.numAttrs() > 0);

        if (matches) {
            result.getResult().add(ZicoUtil.packTraceRecord(symbolRegistry, tr, path, 250));

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
            searchRecords(tr.getChild(i), path + "/" + i, matcher, result, traceTime, matches || recur);
        }
    }


    public TraceRecord getTraceRecord(TraceInfoRecord info, String path, long minMethodTime) {
        try {
            TraceRecord tr = fetchRecord(info, minMethodTime);
            if (path != null && path.trim().length() > 0) {
                for (String p : RE_SLASH.split(path.trim())) {
                    Integer idx = Integer.parseInt(p);
                    if (idx >= 0 && idx < tr.numChildren()) {
                        tr = tr.getChild(idx);
                    } else {
                        throw new ZicoRuntimeException("Child record of path " + path + " not found.");
                    }
                }
            }

            return tr;
        } catch (Exception e) {
            throw new ZicoRuntimeException("Error retrieving trace record.", e);
        }
    }


    private TraceRecord fetchRecord(TraceInfoRecord info, long minMethodTime) throws IOException {
        TraceRecord tr = null;

        if (tr == null) {
            byte[] blob = rds.read(info.getDataOffs(), info.getDataLen());
            ByteArrayInputStream is = new ByteArrayInputStream(blob);
            FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            tr = (TraceRecord) reader.readObject();
            if (minMethodTime > 0) {
                tr = filterByTime(tr, minMethodTime);
            }
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


    public List<MethodRankInfo> methodRank(TraceInfoRecord info, String orderBy, String orderDesc) {
        TraceRecord tr = getTraceRecord(info, "", 0);

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

    @Override
    public void close() throws IOException {
        rds.close();
    }


    public static class ChunkInfo {
        private long offset;
        private int length;

        public ChunkInfo(long offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public long getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }
    }
}
