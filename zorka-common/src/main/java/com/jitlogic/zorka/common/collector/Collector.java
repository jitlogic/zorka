package com.jitlogic.zorka.common.collector;

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

    private volatile TraceChunkStore store;

    private volatile SymbolMapper mapper;

    private boolean skipAgentData;

    private AtomicLong agdCount = new AtomicLong();
    private AtomicLong trcCount = new AtomicLong();

    public Collector(SymbolMapper mapper, TraceChunkStore chunkStore, boolean skipAgentData) {
        this.mapper = mapper;
        this.skipAgentData = skipAgentData;
        this.store = chunkStore;
    }

    public AgentSession getSession(String sessionId, boolean reset) {
        if (reset) {
            sessions.put(sessionId, new AgentSession());
        }
        return sessions.get(sessionId);
    }

    public void handleAgentData(String sessionId, boolean reset, byte[] data) {
        if (skipAgentData) return;
        AgentSession ses = getSession(sessionId, reset);
        if (ses == null) throw new NoSuchSessionException(sessionId);
        synchronized (ses) {
            ses.handleAgentData(data, mapper);
            agdCount.incrementAndGet();
        }
    }

    public void handleTraceData(String sessionId, String traceId, int chunkNum, byte[] data) {
        AgentSession ses = getSession(sessionId, false);
        if (ses == null) throw new NoSuchSessionException(sessionId);
        synchronized (ses) {
            ses.handleTraceData(data, traceId, chunkNum, store);
            trcCount.incrementAndGet();
        }
    }

    public long getAgdCount() {
        return agdCount.longValue();
    }

    public long getTrcCount() {
        return trcCount.longValue();
    }

    public int cleanup(long timeout) {
        int nsessions = 0;
        long t = System.currentTimeMillis();
        for (Map.Entry<String,AgentSession> e : sessions.entrySet()) {
            if (e.getValue().getTstamp() + timeout < t) {
                sessions.remove(e.getKey());
                nsessions++;
            }
        }
        return nsessions;
    }

    public void remove(String sid) {
        sessions.remove(sid);
    }
}
