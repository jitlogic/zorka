package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Local memory collector. It simulates HTTP client, thus agent is forced to perform all
 * serialization/deserialization steps which is not very efficient. On the other hand it
 * is very useful for testing agent-collector integration and collector-side indexing code
 *
 */
public class Collector {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<String, AgentSession>();

    private volatile int tsnum;

    private volatile TraceChunkStore store;

    private volatile SymbolMapper mapper;

    private boolean skipAgentData;

    private AtomicLong agdCount = new AtomicLong();
    private AtomicLong trcCount = new AtomicLong();

    public Collector(int tsnum, SymbolMapper mapper, TraceChunkStore chunkStore, boolean skipAgentData) {
        this.tsnum = tsnum;
        this.mapper = mapper;
        this.skipAgentData = skipAgentData;
        this.store = chunkStore;
    }

    public synchronized void reset(int tsnum, SymbolMapper mapper, TraceChunkStore store) {
        this.tsnum = tsnum;
        this.mapper = mapper;
        this.store = store;
    }

    public AgentSession getSession(String sessionId, boolean reset) {
        if (reset) {
            sessions.put(sessionId, new AgentSession(sessionId, tsnum, mapper, store));
        }
        return sessions.get(sessionId);
    }

    public void handleAgentData(String sessionId, boolean reset, byte[] data) {
        if (skipAgentData) return;
        AgentSession ses = getSession(sessionId, reset);
        if (ses == null) throw new ZorkaRuntimeException("No such session: " + sessionId);
        synchronized (ses) {
            if (ses.getTsnum() != tsnum) ses.reset(tsnum, mapper, store);
            ses.handleAgentData(data);
            agdCount.incrementAndGet();
        }
    }

    public void handleTraceData(String sessionId, String traceId, int chunkNum, byte[] data) {
        AgentSession ses = getSession(sessionId, false);
        if (ses == null) throw new ZorkaRuntimeException("No such session: " + sessionId);
        synchronized (ses) {
            if (ses.getTsnum() != tsnum) ses.reset(tsnum, mapper, store);
            ses.handleTraceData(data, traceId, chunkNum);
            trcCount.incrementAndGet();
        }
    }

    public long getAgdCount() {
        return agdCount.longValue();
    }

    public long getTrcCount() {
        return trcCount.longValue();
    }
}
