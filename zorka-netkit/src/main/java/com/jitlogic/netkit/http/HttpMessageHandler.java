package com.jitlogic.netkit.http;

import com.jitlogic.netkit.ProtocolException;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HttpMessageHandler implements HttpListener, HttpMessageListener {

    private HttpConfig config;
    private HttpMessage message = null;
    private HttpMessageListener listener;

    public HttpMessageHandler(HttpConfig config, HttpMessageListener listener) {
        this.config = config;
        this.listener = listener;
    }

    @Override
    public HttpMessageHandler request(SelectionKey key, String httpVersion, HttpMethod method, String uri, String query) {
        message = new HttpMessage(false);
        message.setVersion(httpVersion);
        message.setMethod(method);
        message.setUri(uri);
        message.setQuery(query);
        return this;
    }

    @Override
    public HttpMessageHandler response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        message = new HttpMessage(true);
        message.setVersion(httpVersion);
        message.setStatus(status);
        return this;
    }

    @Override
    public HttpMessageHandler header(SelectionKey key, String name, String value) {
        if (message == null) {
            throw new ProtocolException("Illegal state: request() or response() should be called first.", null);
        }
        Map<String, List<String>> hdrs = message.getHeaders();
        if (!hdrs.containsKey(name)) hdrs.put(name, new ArrayList<String>());
        hdrs.get(name).add(value);
        return this;
    }

    @Override
    public HttpMessageHandler body(SelectionKey key, Object... parts) {
        for (Object part : parts) {
            message.getBodyParts().add(part);
        }
        return this;
    }

    @Override
    public HttpMessageHandler finish(SelectionKey key) {
        listener.submit(key, message);
        message = null;
        return this;
    }

    @Override
    public HttpMessageHandler error(SelectionKey key, int status, String message, Object data, Throwable e) {
        throw new ProtocolException(message + ": '" + data + "'", null);
    }

    @Override
    public void submit(SelectionKey key, HttpMessage msg) {
        HttpEncoder encoder = new HttpEncoder(config);
        if (msg.isResponse()) {
            encoder.response(key, msg.getVersion(), msg.getStatus(), "");
        } else {
            encoder.request(key, msg.getVersion(), msg.getMethod(), msg.getUri(), msg.getQuery());
        }

        for (Map.Entry<String,List<String>> e : msg.getHeaders().entrySet()) {
            for (String val : e.getValue()) {
                encoder.header(key, e.getKey(), val);
            }
        }

        if (msg.getBodyParts().size() > 0) {
            encoder.body(key, msg.getBodyParts().toArray());
        }

        encoder.finish(key);
    }
}
