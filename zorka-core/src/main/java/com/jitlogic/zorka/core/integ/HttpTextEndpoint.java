package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpTextEndpoint implements ZorkaSubmitter<String>, HttpHandler {

    private static Logger log = LoggerFactory.getLogger(HttpTextEndpoint.class);

    private String uri;

    private volatile byte[] output = "# no data collected yet ...\n".getBytes();

    public HttpTextEndpoint(String uri) {
        this.uri = uri;
    }

    @Override
    public synchronized boolean submit(String item) {
        if (log.isTraceEnabled()) {
            log.trace("Submitted output:\n{}\n--------------------------", item);
        }

        this.output = item.getBytes();
        return true;
    }

    public boolean matches(String vhost, String uri) {
        return this.uri.equals(uri);
    }

    @Override
    public HttpMessage handle(HttpMessage message) {
        return HttpMessage.RESP(200, output, HttpProtocol.H_CONTENT_TYPE, "text/plain").setStatusLine("OK");
    }
}
