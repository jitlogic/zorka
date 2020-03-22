package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.http.*;
import com.jitlogic.zorka.common.tracedata.PerfTextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class HttpTextEndpoint implements ZorkaSubmitter<PerfTextChunk>, HttpHandler {

    private static Logger log = LoggerFactory.getLogger(HttpTextEndpoint.class);

    private String uri;
    private long horizon;
    private String separator;

    private Map<String,PerfTextChunk> chunks = new ConcurrentHashMap<String,PerfTextChunk>();

    public HttpTextEndpoint(String uri, long horizon, String separator) {
        this.uri = uri;
        this.horizon = horizon;
        this.separator = separator;
    }

    @Override
    public synchronized boolean submit(PerfTextChunk item) {
        if (log.isTraceEnabled()) {
            log.trace("Submitted output:\n{}\n--------------------------", item);
        }

        chunks.put(item.getLabel(), item);

        return true;
    }

    public boolean matches(String vhost, String uri) {
        return this.uri.equals(uri);
    }

    @Override
    public HttpMessage handle(HttpMessage message) {
        long tstamp = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,PerfTextChunk> e : chunks.entrySet()) {
            PerfTextChunk p = e.getValue();
            if (p.getTstamp()+horizon > tstamp) {
                sb.append(new String(p.getData(), Charset.defaultCharset()));
                sb.append(separator);
            }
        }
        return HttpMessage.RESP(200, sb.toString().getBytes(), HttpProtocol.H_CONTENT_TYPE, "text/plain").setStatusLine("OK");
    }
}
