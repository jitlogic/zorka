package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceRecordFlags;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TraceDataExtractingProcessor implements TraceDataProcessor {

    private TraceDataResult root;
    private TraceDataResult top;

    private Map<Integer,String> symbols;
    private Map<Integer,String> methods;

    private Map<Long,TraceDataResultException> exceptions = new TreeMap<Long, TraceDataResultException>();

    public TraceDataExtractingProcessor(Map<Integer,String> symbols, Map<Integer,String> methods) {
        this.symbols = symbols;
        this.methods = methods;
    }

    @Override
    public void stringRef(int remoteId, String s) {
        // ignored
    }

    @Override
    public void methodRef(int remoteId, int classId, int methodId, int signatureId) {
        // ignored
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        top = new TraceDataResult(top);
        top.setChunkPos(pos);
        top.setTstart(tstart);
        top.setMethod(methods.get(methodId));
        if (root == null) root = top;
    }

    @Override
    public void traceEnd(long tstop, long calls, int flags) {
        top.setTstop(tstop);
        top.setCalls(calls);
        if (0 != (flags & TraceRecordFlags.TF_ERROR_MARK)) top.setErrors(1);
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        top.setTstamp(tstamp);
        top.setTraceType(symbols.get(ttypeId));
        top.setSpanId(spanId);
        top.setParentId(parentId);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        String attrName = symbols.get(attrId);
        if (attrName != null) {
            top.setAttr(attrName, ""+attrVal);
        }
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        String traceType = symbols.get(ttypeId);
        String attrName = symbols.get(attrId);
        if (traceType != null && attrName != null) {
            for (TraceDataResult tdr = top; tdr != null; tdr = tdr.getParent()) {
                if (traceType.equals(tdr.getTraceType())) {
                    tdr.setAttr(attrName, ""+attrVal);
                    break;
                }
            }
        }
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        TraceDataResultException ex = new TraceDataResultException();
        ex.setId(excId);
        ex.setClassName(symbols.get(classId));
        ex.setMessage(message);
        if (cause != 0) ex.setCause(exceptions.get(cause));
        for (int[] si : stackTrace) {
            String className = symbols.get(si[0]);
            String methodName = symbols.get(si[1]);
            String fileName = symbols.get(si[2]);
            int fileLine = si[3];
            ex.getStack().add(String.format("%s.%s (%s:%d)", className, methodName, fileName, fileLine));
        }
        // TODO attrs
        if (ex.getId() != 0) exceptions.put(ex.getId(), ex);
        top.setException(ex);
    }

    @Override
    public void exceptionRef(long excId) {
        top.setException(exceptions.get(excId));
    }

    public int getStackDepth() {
        int depth = 0;
        for (TraceDataResult r = top; r != null; r = r.getParent()) depth++;
        return depth;
    }

    public TraceDataResult getRoot() {
        return root;
    }
}
