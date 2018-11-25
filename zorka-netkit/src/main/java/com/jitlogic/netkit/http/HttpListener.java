package com.jitlogic.netkit.http;

import java.nio.channels.SelectionKey;

/**
 * Unified interface for HTTP message handling.
 */
public interface HttpListener extends HttpErrorHandler {

    /** Handles request line. */
    HttpListener request(SelectionKey key, String httpVersion, HttpMethod method, String uri, String query);

    /** Handles response line. */
    HttpListener response(SelectionKey key, String httpVersion, int status, String statusMessage);

    /** Handles (single) header. */
    HttpListener header(SelectionKey key, String name, String value);

    /** Handles (part of) request body. */
    HttpListener body(SelectionKey key, Object...parts);

    /** This method is called when HTTP message is complete. */
    HttpListener finish(SelectionKey key);

}
