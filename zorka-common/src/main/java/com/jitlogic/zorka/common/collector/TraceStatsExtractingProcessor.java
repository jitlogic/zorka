package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceRecordFlags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TraceStatsExtractingProcessor implements TraceDataProcessor {

    private Map<Integer,TraceStatsResult> stats = new HashMap<Integer, TraceStatsResult>();
    private Stack<Integer> mids = new Stack<Integer>();
    private Stack<Long> tstamps = new Stack<Long>();

    public Map<Integer, TraceStatsResult> getStats() {
        return stats;
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        // nothing here
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        // nothing here
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        mids.push(methodId);
        tstamps.push(tstart);
    }

    @Override
    public void traceEnd(int pos, long tstop, long calls, int flags) {
        if (!tstamps.isEmpty()) {
            int mid = mids.pop();
            long tstart = tstamps.pop();
            TraceStatsResult tsr = stats.get(mid);
            if (tsr == null) {
                tsr = new TraceStatsResult();
                tsr.setMid(mid);
                stats.put(mid, tsr);
            }
            tsr.addCalls(calls);
            tsr.addRecs(1);
            if (0 != (flags & TraceRecordFlags.TF_ERROR_MARK)) tsr.addErrors(1);
            tsr.addDuration(tstop-tstart);
        }
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        // nothing here
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        // nothing here
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        // nothing here
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        // nothing here
    }

    @Override
    public void exceptionRef(long excId) {
        // nothing here
    }
}
