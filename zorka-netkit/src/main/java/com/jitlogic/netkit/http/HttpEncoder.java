/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.netkit.http;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.InvalidStateException;
import com.jitlogic.netkit.NetCtx;
import com.jitlogic.netkit.util.DynamicBytes;
import com.jitlogic.netkit.util.TextUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Date;

import static com.jitlogic.netkit.http.HttpProtocol.*;

public class HttpEncoder implements HttpListener {

    private static final int REQ_LINE_SENT       = 0x01;
    private static final int RESP_LINE_SENT      = 0x02;
    private static final int HEADERS_SENT        = 0x04;
    private static final int BODY_SENT           = 0x08;
    private static final int FINISHED            = 0x10;

    private static final int CONTENT_LENGTH_SENT = 0x0100;
    private static final int SERVER_NAME_SENT    = 0x0200;
    private static final int DATE_SENT           = 0x0400;
    private static final int USER_AGENT_SENT     = 0x0800;
    private static final int CONNECTION_SENT     = 0x1000;
    private static final int CONNECTION_CLOSE    = 0x2000;

    private int state = 0;
    private DynamicBytes buf = new DynamicBytes(256);
    private ByteBuffer body = NetCtx.NULL;
    private BufHandler output;
    private HttpMethod method;
    private HttpConfig config;

    public HttpEncoder(HttpConfig config) {
        this.config = config;
    }

    public HttpEncoder(HttpConfig config, BufHandler output) {
        this.config = config;
        this.output = output;
    }

    private boolean gsf(int sbits) {
        return 0 != (state & sbits);
    }

    private void chki(boolean cond, int sbits) {
        if (cond) state |= sbits;
    }

    private void chkh(String expected, String name, int sbits) {
        if (expected.equalsIgnoreCase(name)) state |= sbits;
    }

    @Override
    public HttpEncoder request(SelectionKey key, String httpVersion, HttpMethod method, String url, String query) {
        method.appendMethod(buf);
        buf.append(SP).append(url);
        if (query != null) buf.append("?").append(query);
        buf.append(SP).append(HTTP_1_1).append(CRLF);
        this.method = method;
        state |= REQ_LINE_SENT;
        return this;
    }

    @Override
    public HttpEncoder response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        buf.append(HttpStatus.valueOf(status).getInitialLineBytes());
        state |= RESP_LINE_SENT;

        chki(emptyBodyExpected(status), CONTENT_LENGTH_SENT);
        return this;
    }

    @Override
    public HttpEncoder header(SelectionKey key, String name, String value) {
        if (0 == (state & (REQ_LINE_SENT|RESP_LINE_SENT))) {
            throw new InvalidStateException("Cannot send headers without initiation line.");
        }

        if (gsf(HEADERS_SENT)) {
            throw new InvalidStateException("Cannot send headers anymore.");
        }

        buf.append(TextUtil.camelCase(name)).append(": ").append(value).append(CRLF);

        chkh(H_CONTENT_LENGTH, name, CONTENT_LENGTH_SENT);
        chkh(H_DATE, name, DATE_SENT);
        chkh(H_SERVER, name, SERVER_NAME_SENT);
        chkh(H_USER_AGENT, name, USER_AGENT_SENT);
        chkh(H_CONNECTION, name, CONNECTION_SENT);

        if (H_CONNECTION.equalsIgnoreCase(name) && "close".equalsIgnoreCase(value)) {
            state |= CONNECTION_CLOSE;
        }

        return this;
    }

    @Override
    public HttpEncoder body(SelectionKey key, Object...parts) {
        if (0 == (state & (REQ_LINE_SENT|RESP_LINE_SENT))) {
            throw new InvalidStateException("Cannot send body without initiation line.");
        }

        if (0 != (state & CONTENT_LENGTH_SENT) && 0 != (state & BODY_SENT)) {
            throw new InvalidStateException("Body with fixed content length already sent.");
        }

        if (method != null && !method.hasBody()) {
            throw new InvalidStateException("This request method does not have body.");
        }

        ByteBuffer body = (ByteBuffer)parts[0]; // TODO coerce to body or store refs somewhere

        if (0 == (state & CONTENT_LENGTH_SENT)) {
            // TODO if content length previously sent, check for proper file size
            header(key, H_CONTENT_LENGTH, "" + body.remaining());
        }
        finishHeaders(key);

        state |= BODY_SENT;

        this.body = body;
        return this;
    }

    private void finishHeaders(SelectionKey key) {
        if (!gsf(HEADERS_SENT)) {
            if (0 == (state & CONTENT_LENGTH_SENT)) header(key, H_CONTENT_LENGTH, "0");
            if (0 == (state & DATE_SENT)) header(key, H_DATE, HttpProtocol.dateFormat(new Date()));
            if (0 != (state & RESP_LINE_SENT) && 0 == (state & SERVER_NAME_SENT))
                header(key, H_SERVER, "httpkit/2.90.1");
            if (0 != (state & REQ_LINE_SENT) && 0 == (state & USER_AGENT_SENT))
                header(key, H_USER_AGENT, "httpkit/2.90.1");
            if (0 == (state & CONNECTION_SENT)) {
                if (keepAlive()) {
                    header(key, H_CONNECTION, "keep-alive");
                    header(key, H_KEEP_ALIVE, config.getKeepAliveString());
                } else {
                    header(key, H_CONNECTION, "close");
                }
            }
            buf.append(CRLF);
            state |= HEADERS_SENT;
        }
    }

    private boolean keepAlive() {
        // TODO check if client requested Connection: close, also keepalive disabled by default for HTTP/1.0
        return !gsf(CONNECTION_CLOSE) && gsf(REQ_LINE_SENT|RESP_LINE_SENT) && config.getKeepAliveTimeout() > 0;
    }

    @Override
    public HttpEncoder finish(SelectionKey key) {
        finishHeaders(key);
        ByteBuffer bb = ByteBuffer.wrap(buf.get(), 0, buf.length());
        BufHandler out = output != null ? output : NetCtx.fromKey(key).getOutput();
        out.submit(key, true, bb, body, keepAlive() ? NetCtx.FLUSH : NetCtx.CLOSE);
        state |= FINISHED;
        return this;
    }

    public void reset() {
        buf.reset();
        state = 0;
    }

    @Override
    public HttpEncoder error(SelectionKey key, int status, String message, Object data, Throwable e) {
        reset();
        response(key, HTTP_1_1, status, null);
        header(key, H_CONTENT_TYPE, "text/plain");
        body(key, ByteBuffer.wrap(message.getBytes()));
        finish(key);
        return this;
    }
}
