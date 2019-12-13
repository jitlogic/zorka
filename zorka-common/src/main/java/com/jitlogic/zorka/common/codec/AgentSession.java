package com.jitlogic.zorka.common.codec;

import com.jitlogic.zorka.cbor.CborDataWriter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentSession {

    private SymbolRegistry symbols;
    private String sessionId;
    private CborDataWriter cborWriter;

    private Map<String,TraceDataTranslator> tcache = new ConcurrentHashMap<String, TraceDataTranslator>();

    public AgentSession(String sessionId) {
        this.symbols = new SymbolRegistry();
        this.sessionId = sessionId;
        cborWriter = new CborDataWriter(1024 * 1024, 1024 * 1024);
    }

    public SymbolRegistry getSymbols() {
        return symbols;
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized void handleAgentData(byte[] data) {
        AgentDataReader ar = new AgentDataReader(new CborBufReader(data), new TraceDataTranslator(this));
        ar.run();
    }

    public synchronized TraceChunkData handleTraceData(byte[] data, TraceChunkData tcd) {
        String tid = tcd.getTraceIdHex() + tcd.getSpanIdHex();
        TraceDataTranslator translator = tcache.get(tid);
        if (translator == null) {
            translator = new TraceDataTranslator(this);
        } else {
            // TODO Each trace that spans onto next chunk needs to have its start offset set to 0
            //for (ChunkMetadata d : translator.getTraceStackRecs()) {
            //    d.setStartOffs(0);
            //}
        }

        TraceDataReader tdr = new TraceDataReader(new CborBufReader(data), translator);
        cborWriter.reset();
        tdr.run();

        long stackSize = translator.getStackDepth();

        return tcd;
    }

}
