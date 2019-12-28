package com.jitlogic.zorka.common.cbor;


import java.util.List;
import java.util.Map;


public class TraceDataScanner implements BufferedTraceDataProcessor {

    private TraceDataScannerVisitor visitor;
    private BufferedTraceDataProcessor output;

    public TraceDataScanner(TraceDataScannerVisitor visitor) {
        this(visitor, null);
    }

    public TraceDataScanner(TraceDataScannerVisitor visitor, BufferedTraceDataProcessor output) {
        this.visitor = visitor;
        this.output = output;
    }

    private int addSymbol(int id) {
        return visitor.symbolId(id);
    }

    private int addMethod(int mid) {
        return visitor.methodId(mid);
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        if (output != null) output.stringRef(symbolId, symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        if (output != null) output.methodRef(symbolId, classId, methodId, signatureId);
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        methodId = addMethod(methodId);
        if (output != null) output.traceStart(pos, tstart, methodId);
    }

    @Override
    public void traceEnd(int pos, long tstop, long calls, int flags) {
        if (output != null) output.traceEnd(pos, tstop, calls, flags);
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        ttypeId = addSymbol(ttypeId);
        if (output != null) output.traceBegin(tstamp, ttypeId, spanId, parentId);
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        attrId = addSymbol(attrId);
        if (output != null) output.traceAttr(attrId, attrVal);
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        ttypeId = addSymbol(ttypeId);
        attrId = addSymbol(attrId);
        if (output != null) output.traceAttr(ttypeId, attrId, attrVal);
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        classId = addSymbol(classId);
        for (int[] si : stackTrace) {
            si[0] = addSymbol(si[0]); // classId
            si[1] = addSymbol(si[1]); // methodId
            si[2] = addSymbol(si[2]); // fileId
        }
        if (output != null) output.exception(excId, classId, message, cause, stackTrace, attrs);
    }

    @Override
    public void exceptionRef(long excId) {
        if (output != null) output.exceptionRef(excId);
    }

    @Override
    public int size() {
        return output.size();
    }

    @Override
    public byte[] chunk(int offs, int len) {
        return output.chunk(offs, len);
    }
}
