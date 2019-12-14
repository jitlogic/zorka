package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;


public class AgentSession implements TraceDataScannerVisitor {

    private SymbolRegistry registry;
    private SymbolMapper mapper;
    private TraceChunkStore store;

    /** Map: agentSymbolId -> collectorSymbolId */
    private Map<Integer,Integer> symbolsMap = new HashMap<Integer, Integer>();

    /** Map: agentMethodId -> collectorMethodId */
    private Map<Integer,Integer> methodsMap = new HashMap<Integer, Integer>();

    private String sessionId;

    public AgentSession(String sessionId, SymbolMapper mapper, TraceChunkStore store) {
        this.registry = new SymbolRegistry();
        this.sessionId = sessionId;
        this.mapper = mapper;
        this.store = store;
    }

    public SymbolRegistry getRegistry() {
        return registry;
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized void handleAgentData(byte[] data) {
        AgentDataHandler adh = new AgentDataHandler(registry);
        new TraceDataReader(new CborDataReader(data), adh).run();
        symbolsMap.putAll(mapper.newSymbols(adh.getNewSymbols()));
        methodsMap.putAll(mapper.newMethods(adh.getNewMethods()));
    }

    public synchronized void handleTraceData(byte[] data, String traceId, int chunkNum) {
        long tid1 = new BigInteger(traceId.substring(0,16), 16).longValue();
        long tid2 = new BigInteger(traceId.substring(16), 16).longValue();
        TraceChunkData tcd = new TraceChunkData(tid1, tid2, 0, 0, chunkNum);
        CborDataWriter cbw = new CborDataWriter(data.length+1024, 4096);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        TraceMetadataIndexer tme = new TraceMetadataIndexer(registry, tcd, tdw);
        TraceDataScanner ssp = new TraceDataScanner(this, tme);
        new TraceDataReader(new CborDataReader(data), ssp).run();
        tcd.setTraceData(cbw.toByteArray());
        store.add(tcd);
    }

    @Override
    public int symbolId(int symbolId) {
        Integer rslt = symbolsMap.get(symbolId);
        return rslt != null ? rslt : 0;
    }

    @Override
    public int methodId(int methodId) {
        Integer rslt = methodsMap.get(methodId);
        return rslt != null ? rslt : 0;
    }
}
