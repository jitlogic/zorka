package com.jitlogic.zorka.common.codec;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.Map;

public class TraceDataTranslator implements TraceDataProcessor, AgentDataProcessor {

    public final static int TICKS_IN_SECOND = 1000000000/65536;

    private AgentSession session;
    private SymbolRegistry symbols;

    public TraceDataTranslator(AgentSession session) {
        this.session = session;
        this.symbols = session.getSymbols();
    }

    @Override
    public int defStringRef(int remoteId, String s, byte type) {
        symbols.put(remoteId, s);
        return remoteId;
    }

    @Override
    public int defMethodRef(int remoteId, int classId, int methodId, int signatureId) {
        symbols.putMethod(remoteId, classId, methodId, signatureId);
        return remoteId;
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {

    }

    @Override
    public void traceEnd(long tstop, long calls) {

    }

    @Override
    public void traceBegin(long tstamp, long spanId, long parentId) {

    }

    @Override
    public void traceFlags(int flags) {

    }

    @Override
    public void traceAttr(Map<Object, Object> data) {

    }

    @Override
    public void exceptionRef(int ref) {

    }

    @Override
    public void exception(ExceptionData ex) {

    }

    @Override
    public void commit() {

    }

    public int getStackDepth() {
        return 0;
    }
}
