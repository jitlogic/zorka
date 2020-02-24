package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicMethod;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.collector.SymbolDataExtractor.extractSymbolData;


public class AgentSession {

    private SymbolRegistry agentSymbols = new SymbolRegistry();

    private volatile long tstamp = System.currentTimeMillis();

    public SymbolRegistry getRegistry() {
        return agentSymbols;
    }

    public synchronized void handleAgentData(byte[] data) {
        AgentDataHandler adh = new AgentDataHandler(agentSymbols);
        new TraceDataReader(new CborDataReader(data), adh).run();
        tstamp = System.currentTimeMillis();
    }

    public synchronized void handleTraceData(byte[] data, String traceId, int chunkNum, TraceChunkStore store) {
        long tid1 = new BigInteger(traceId.substring(0,16), 16).longValue();
        long tid2 = (traceId.length() > 16) ? new BigInteger(traceId.substring(16), 16).longValue() : 0L;
        TraceChunkData tcd = new TraceChunkData(tid1, tid2, 0, 0, chunkNum); // TODO continuation here
        CborDataWriter cbw = new CborDataWriter(data.length+1024, 4096);
        TraceDataWriter tdw = new TraceDataWriter(cbw);
        TraceMetadataIndexer tme = new TraceMetadataIndexer(agentSymbols, tdw);
        tme.init(tid1, tid2, chunkNum);
        new TraceDataReader(new CborDataReader(data), tme).run();
        //byte[] traceData = cbw.toByteArray();
        //tcd.setTraceData(ZorkaUtil.gzip(traceData));
        //tcd.setSymbolData(extractSymbolData(agentSymbols, traceData));
        List<TraceChunkData> result = tme.getChunks();
        store.addAll(result);
        tstamp = System.currentTimeMillis();
    }

    public long getTstamp() {
        return tstamp;
    }
}
