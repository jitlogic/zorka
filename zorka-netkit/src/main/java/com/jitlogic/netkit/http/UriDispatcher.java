package com.jitlogic.netkit.http;

import java.nio.channels.SelectionKey;
import java.util.List;

public class UriDispatcher implements HttpListener {

    public static final String INVALID_STATE = "invalid state";

    private final HttpConfig config;
    private volatile HttpListener listener;
    private final List<UrlEndpoint> endpoints;

    public UriDispatcher(HttpConfig config, List<UrlEndpoint> endpoints) {
        this.config = config;
        this.endpoints = endpoints;
    }

    @Override
    public HttpListener request(SelectionKey key, String httpVersion, HttpMethod method, String uri, String query) {

        if (listener == null && !dispatch(uri)) {
            error(key, 404, "not found", uri, null);
            return this;
        }

        return listener.request(key, httpVersion, method, uri, query);
    }

    private synchronized boolean dispatch(String uri) {
        for (UrlEndpoint e : endpoints) {
            if (e.matches("localhost", uri)) {
                listener = e.getListener();
                return true;
            }
        }
        return false;
    }

    @Override
    public HttpListener response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        return error(key, 500, INVALID_STATE, "RESP(" + status + ")", null);
    }

    @Override
    public HttpListener header(SelectionKey key, String name, String value) {
        if (listener != null) {
            return listener.header(key, name, value);
        } else {
            return error(key, 500, INVALID_STATE, "HEADER(" + name + ")", null);
        }
    }

    @Override
    public HttpListener body(SelectionKey key, Object...parts) {
        if (listener != null) {
            return listener.body(key, parts);
        } else {
            return error(key, 500, INVALID_STATE, "BODY(...)", null);
        }
    }

    @Override
    public HttpListener finish(SelectionKey key) {
        if (listener != null) {
            return listener.finish(key);
        } else {
            return error(key, 500, INVALID_STATE, "FINISH()", null);
        }
    }

    @Override
    public HttpListener error(SelectionKey key, int status, String message, Object data, Throwable e) {
        if (listener != null) {
            return listener.error(key, status, message, data, e);
        } else {
            HttpProtocolHandler.errorResponse(config, key, status, message);
            return this;
        }
    }
}
