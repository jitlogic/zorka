package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicMethod;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AgentSession implements TraceDataScannerVisitor {

    private SymbolRegistry agentSymbols;

    private volatile long tstamp;

    private volatile int tsnum;
    private volatile SymbolMapper mapper;
    private volatile TraceChunkStore store;

    /** Map: agentSymbolId -> collectorSymbolId */
    private Map<Integer,Integer> symbolsMap = new HashMap<Integer, Integer>();

    /** Map: agentMethodId -> collectorMethodId */
    private Map<Integer,Integer> methodsMap = new HashMap<Integer, Integer>();

    private String sessionId;

    public AgentSession(String sessionId, int tsnum, SymbolMapper mapper, TraceChunkStore store) {
        this.agentSymbols = new SymbolRegistry();
        this.sessionId = sessionId;
        this.tsnum = tsnum;
        this.mapper = mapper;
        this.store = store;
        this.tstamp = System.currentTimeMillis();
    }

    public synchronized void reset(int tsnum, SymbolMapper mapper, TraceChunkStore store) {
        this.tsnum = tsnum;
        this.mapper = mapper;
        this.store = store;
        symbolsMap.clear();
        methodsMap.clear();
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
        Map<Integer, Integer> mappedSymbols = mapper.newSymbols(newSymbols);
        symbolsMap.putAll(mappedSymbols);
        Map<Integer, SymbolicMethod> newMethods = adh.getNewMethods();
        for (Map.Entry<Integer,SymbolicMethod> e : newMethods.entrySet()) {
            SymbolicMethod sm = e.getValue();
            if (symbolsMap.containsKey(sm.getClassId())) sm.setClassId(symbolsMap.get(sm.getClassId()));
            if (symbolsMap.containsKey(sm.getMethodId())) sm.setMethodId(symbolsMap.get(sm.getMethodId()));
            if (symbolsMap.containsKey(sm.getSignatureId())) sm.setSignatureId(symbolsMap.get(sm.getSignatureId()));
        }
        Map<Integer, Integer> mappedMethods = mapper.newMethods(newMethods);
        methodsMap.putAll(mappedMethods);
        this.tstamp = System.currentTimeMillis();
    }

    public synchronized void handleTraceData(byte[] data, String traceId, int chunkNum) {
        long tid1 = new BigInteger(traceId.substring(0,16), 16).longValue();
        long tid2 = (traceId.length() > 16) ? new BigInteger(traceId.substring(16), 16).longValue() : 0L;
        TraceChunkData tcd = new TraceChunkData(tid1, tid2, 0, 0, chunkNum); // TODO continuation here
        CborDataWriter cbw = new CborDataWriter(data.length+1024, 4096);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        TraceDataScanner ssp = new TraceDataScanner(this, tdw);
        TraceMetadataIndexer tme = new TraceMetadataIndexer(agentSymbols, ssp);
        tme.init(tid1, tid2, chunkNum);
        new TraceDataReader(new CborDataReader(data), tme).run();
        tcd.setTraceData(ZorkaUtil.gzip(cbw.toByteArray()));
        List<TraceChunkData> result = tme.getChunks();
        store.addAll(result);
        this.tstamp = System.currentTimeMillis();
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

    public int getTsnum() {
        return tsnum;
    }
}
