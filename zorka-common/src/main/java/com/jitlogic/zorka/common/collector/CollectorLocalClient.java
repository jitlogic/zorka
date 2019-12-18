package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.http.HttpMessage;

public class CollectorLocalClient implements HttpHandler {

    private Collector collector;

    public CollectorLocalClient(Collector collector) {
        this.collector = collector;
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

    private HttpMessage handleSubmitAgd(HttpMessage req) {
        String sessionId = req.getHeader("X-Zorka-Session-ID");
        String sessionReset = req.getHeader("X-Zorka-Session-Reset");
        if (sessionId == null) return HttpMessage.RESP(400, "Missing header: X-Zorka-Session-ID");
        collector.handleAgentData(sessionId, "true".equalsIgnoreCase(sessionReset), req.getBody());
        return HttpMessage.RESP(200, "OK");
    }

    private HttpMessage handleSubmitTrc(HttpMessage req) {
        String sessionId = req.getHeader("X-Zorka-Session-ID");
        String traceId = req.getHeader("X-Zorka-Trace-ID");
        if (sessionId == null) return HttpMessage.RESP(400, "Missing header: X-Zorka-Session-ID");
        collector.handleTraceData(sessionId, traceId, 0, req.getBody()); // TODO chunk num as header (or 0)
        return HttpMessage.RESP(200, "OK");
    }


}
