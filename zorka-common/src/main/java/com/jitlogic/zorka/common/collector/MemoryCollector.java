package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.http.HttpMessage;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Local memory collector. It simulates HTTP client, thus agent is forced to perform all
 * serialization/deserialization steps which is not very efficient. On the other hand it
 * is very useful for testing agent-collector integration and collector-side indexing code
 *
 */
public class MemoryCollector implements HttpHandler {

    private Map<String, AgentSession> sessions = new HashMap<String, AgentSession>();
    private MemoryChunkStore store;

    private SymbolRegistry registry;
    private boolean skipAgentData;

    private int agdCount;
    private int trcCount;

    public MemoryCollector() {
        this(new SymbolRegistry(), false);
    }

    public MemoryCollector(SymbolRegistry registry, boolean skipAgentData) {
        this.registry = registry;
        this.skipAgentData = skipAgentData;
        this.store = new MemoryChunkStore();
    }

    private AgentSession getSession(String sessionId, boolean reset) {
        if (reset) {
            sessions.put(sessionId, new AgentSession(sessionId, registry, store));
        }
        return sessions.get(sessionId);
    }

    @Override
    public HttpMessage handle(HttpMessage req) {
        try {
            if ("/agent/submit/agd".equals(req.getUri())) return handleSubmitAgd(req);
            if ("/agent/submit/trc".equals(req.getUri())) return handleSubmitTrc(req);

            System.out.println("UNKNOWN: " + req);
            return HttpMessage.RESP(404, "Not implemented.");

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            return HttpMessage.RESP(500, "Internal error.");
        }
    }

    public HttpMessage handleSubmitAgd(HttpMessage req) {
        if (skipAgentData) return HttpMessage.RESP(200, "OK");

        String sessionId = req.getHeader("X-Zorka-Session-ID");
        String sessionReset = req.getHeader("X-Zorka-Session-Reset");

        if (sessionId == null) return HttpMessage.RESP(400, "Missing header: X-Zorka-Session-ID");

        AgentSession ses = getSession(sessionId, "true".equalsIgnoreCase(sessionReset));
        if (ses == null) return HttpMessage.RESP(401, "No such session.");

        agdCount++;
        ses.handleAgentData(req.getBody());

        return HttpMessage.RESP(200, "OK");
    }

    public HttpMessage handleSubmitTrc(HttpMessage req) {
        String sessionId = req.getHeader("X-Zorka-Session-ID");
        String traceId = req.getHeader("X-Zorka-Trace-ID");

        if (sessionId == null) return HttpMessage.RESP(400, "Missing header: X-Zorka-Session-ID");

        AgentSession ses = getSession(sessionId, false);
        if (ses == null) return HttpMessage.RESP(401, "No such session.");

        trcCount++;
        ses.handleTraceData(req.getBody(), traceId, 0); // TODO chunk num as header (or 0)

        return HttpMessage.RESP(200, "OK");
    }

    public int getAgdCount() {
        return agdCount;
    }

    public int getTrcCount() {
        return trcCount;
    }

    public SymbolRegistry getRegistry() {
        return registry;
    }

    public MemoryChunkStore getStore() {
        return store;
    }
}
