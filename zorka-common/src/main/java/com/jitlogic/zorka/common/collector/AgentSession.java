package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AgentSession implements TraceDataScannerVisitor {

    private SymbolRegistry agentSymbols;
    private SymbolMapper collectorMapper;
    private TraceChunkStore store;

    /** Map: agentSymbolId -> collectorSymbolId */
    private Map<Integer,Integer> symbolsMap = new HashMap<Integer, Integer>();

    /** Map: agentMethodId -> collectorMethodId */
    private Map<Integer,Integer> methodsMap = new HashMap<Integer, Integer>();

    private String sessionId;

    public AgentSession(String sessionId, SymbolMapper mapper, TraceChunkStore store) {
        this.agentSymbols = new SymbolRegistry();
        this.sessionId = sessionId;
        this.collectorMapper = mapper;
        this.store = store;
    }

    public SymbolRegistry getRegistry() {
        return agentSymbols;
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized void handleAgentData(byte[] data) {
        AgentDataHandler adh = new AgentDataHandler(agentSymbols);
        new TraceDataReader(new CborDataReader(data), adh).run();
        Map<Integer, String> newSymbols = adh.getNewSymbols();
        Map<Integer, Integer> mappedSymbols = collectorMapper.newSymbols(newSymbols);
        symbolsMap.putAll(mappedSymbols);
        methodsMap.putAll(collectorMapper.newMethods(adh.getNewMethods()));
    }

    public synchronized void handleTraceData(byte[] data, String traceId, int chunkNum) {
        long tid1 = new BigInteger(traceId.substring(0,16), 16).longValue();
        long tid2 = new BigInteger(traceId.substring(16), 16).longValue();
        TraceChunkData tcd = new TraceChunkData(tid1, tid2, 0, 0, chunkNum); // TODO continuation here
        CborDataWriter cbw = new CborDataWriter(data.length+1024, 4096);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        TraceDataScanner ssp = new TraceDataScanner(this, tdw);
        TraceMetadataIndexer tme = new TraceMetadataIndexer(agentSymbols, ssp);
        tme.init(data, tid1, tid2, chunkNum);
        new TraceDataReader(new CborDataReader(data), tme).run();
        tcd.setTraceData(cbw.toByteArray());
        List<TraceChunkData> result = tme.getChunks();
        store.addAll(result);
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
