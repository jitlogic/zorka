package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.util.*;

public class AgentDataHandler implements TraceDataProcessor {

    private SymbolRegistry registry;
    private Map<Integer,String> newSymbols = new HashMap<Integer,String>();
    private Map<Integer,int[]> newMethods = new HashMap<Integer,int[]>();

    public Map<Integer,String> getNewSymbols() {
        return newSymbols;
    }

    public Map<Integer,int[]> getNewMethods() {
        return newMethods;
    }

    public AgentDataHandler(SymbolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void stringRef(int symbolId, String symbol) {
        if (!registry.hasSymbol(symbolId)) {
            newSymbols.put(symbolId,symbol);
            registry.putSymbol(symbolId, symbol);
        }
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        if (!registry.hasMethod(symbolId)) {
            newMethods.put(symbolId, new int[]{classId,methodId,signatureId});
            registry.putMethod(symbolId, classId, methodId, signatureId);
        }
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void traceEnd(long tstop, long calls, int flags) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void traceBegin(long tstamp, int ttypeId, long spanId, long parentId) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void traceAttr(int attrId, Object attrVal) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void exception(long excId, int classId, String message, long cause, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }

    @Override
    public void exceptionRef(long excId) {
        throw new ZorkaRuntimeException("This is not allowed in agent state data.");
    }
}
