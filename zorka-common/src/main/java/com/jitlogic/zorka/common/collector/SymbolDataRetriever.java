package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.CborDataReader;
import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.cbor.TraceDataReader;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.List;
import java.util.Map;

public class SymbolDataRetriever implements TraceDataProcessor {

    private SymbolRegistry registry = new SymbolRegistry();

    @Override
    public void stringRef(int symbolId, String symbol) {
        registry.putSymbol(symbolId, symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        registry.putMethod(symbolId, classId, methodId, signatureId);
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
    }

    @Override
    public void traceEnd(int pos, long tstop, long calls, int flags) {
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
    }

    @Override
    public void exceptionRef(long excId) {
    }

    public SymbolRegistry getRegistry() {
        return registry;
    }

    public static SymbolRegistry retrieve(byte[] sdata) {
        CborDataReader cdr = new CborDataReader(ZorkaUtil.gunzip(sdata));
        SymbolDataRetriever sdr = new SymbolDataRetriever();
        new TraceDataReader(cdr, sdr).run();
        return sdr.getRegistry();
    }
}
