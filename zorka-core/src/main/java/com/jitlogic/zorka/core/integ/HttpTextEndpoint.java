package com.jitlogic.zorka.core.integ;

import com.jitlogic.netkit.http.UrlEndpoint;
import com.jitlogic.netkit.http.*;
import com.jitlogic.zorka.common.ZorkaSubmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class HttpTextEndpoint implements ZorkaSubmitter<String>, HttpListener, UrlEndpoint {

    private static Logger log = LoggerFactory.getLogger(HttpTextEndpoint.class);

    private String uri;
    private HttpConfig config;

    private volatile byte[] output = "# no data collected yet ...\n".getBytes();

    public HttpTextEndpoint(String uri) {
        this(new HttpConfig(), uri);
    }

    public HttpTextEndpoint(HttpConfig config, String uri) {
        this.config = config;
        this.uri = uri;
    }

    @Override
    public synchronized boolean submit(String item) {
        if (log.isTraceEnabled()) {
            log.trace("Submitted output:\n" + item + "\n--------------------------");
        }

        this.output = item.getBytes();
        return true;
    }

    @Override
    public HttpListener request(SelectionKey key, String httpVersion, HttpMethod method, String uri, String query) {
        return this;
    }

    @Override
    public HttpListener response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        return this;
    }

    @Override
    public HttpListener header(SelectionKey key, String name, String value) {
        return this;
    }

    @Override
    public HttpListener body(SelectionKey key, Object... parts) {
        return this;
    }

    @Override
    public HttpListener finish(SelectionKey key) {

        byte[] b = output;

        HttpEncoder enc = new HttpEncoder(config);
        enc.response(key, HttpProtocol.HTTP_1_1, 200, "OK");
        enc.header(key, HttpProtocol.H_CONTENT_TYPE, "text/plain");
        enc.body(key, ByteBuffer.wrap(b));
        enc.finish(key);

        return this;
    }

    @Override
    public HttpListener error(SelectionKey key, int status, String message, Object data, Throwable e) {
        HttpProtocolHandler.errorResponse(config, key, status, message);
        return this;
    }

    @Override
    public boolean matches(String vhost, String uri) {
        return this.uri.equals(uri);
    }

    @Override
    public HttpListener getListener() {
        return this;
    }
}
