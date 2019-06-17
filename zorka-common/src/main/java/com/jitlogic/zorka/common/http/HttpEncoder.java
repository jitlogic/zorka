/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.zorka.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.http.HttpProtocol.*;

public class HttpEncoder implements HttpHandler {

    private Logger log = LoggerFactory.getLogger(HttpEncoder.class);

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
    private static final int HOST_SENT           = 0x4000;

    private int state = 0;
    private OutputStream os;
    private String uriPrefix;
    private HttpConfig config;

    public HttpEncoder(HttpConfig config, String uriPrefix, OutputStream os) {
        this.uriPrefix = uriPrefix;
        this.config = config;
        this.os = os;
    }

    private void write(String s) {
        try {
            os.write(s.getBytes());
        } catch (IOException e) {
            throw new HttpException("I/O error", 503, "I/O error", null, e);
        }
    }

    private void write(byte[] b) {
        try {
            os.write(b);
        } catch (IOException e) {
            throw new HttpException("I/O error", 503, "I/O error", null, e);
        }
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

    private void requestLine(String httpVersion, HttpMethod method, String url, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(SP).append(uriPrefix).append(url);
        if (query != null) sb.append("?").append(query);
        sb.append(SP).append(httpVersion).append(CRLF);
        if (log.isDebugEnabled()) {
            log.debug("REQ: " + sb.toString());
        }
        write(sb.toString());
        state |= REQ_LINE_SENT;
    }

    private void responseLine(String httpVersion, int status, String statusMessage) {
        String respLine = httpVersion + " " + status + " " + statusMessage;
        write(respLine + CRLF);
        if (log.isDebugEnabled()) {
            log.debug("RES: " + respLine);
        }
        state |= RESP_LINE_SENT;
        chki(emptyBodyExpected(status), CONTENT_LENGTH_SENT);
    }

    private void header(String name, String value) {

        chkh(H_CONTENT_LENGTH, name, CONTENT_LENGTH_SENT);
        chkh(H_DATE, name, DATE_SENT);
        chkh(H_SERVER, name, SERVER_NAME_SENT);
        chkh(H_USER_AGENT, name, USER_AGENT_SENT);
        chkh(H_CONNECTION, name, CONNECTION_SENT);
        chkh(H_HOST, name, HOST_SENT);

        if (H_CONNECTION.equalsIgnoreCase(name) && "close".equalsIgnoreCase(value)) {
            state |= CONNECTION_CLOSE;
        }

        String hdr = TextUtil.camelCase(name) + ": " + value;

        if (log.isDebugEnabled()) {
            log.debug("HDR: " + hdr);
        }

        write(hdr + CRLF);
    }

    private void body(byte[] b) {

        if (b == null) b = new byte[0];

        if (0 == (state & CONTENT_LENGTH_SENT)) {
            // TODO if content length previously sent, check for proper file size
            header(H_CONTENT_LENGTH, "" + b.length);
        }

        finishHeaders();

        if (b.length > 0) write(b);

        state |= BODY_SENT;
    }

    private void finishHeaders() {
        if (!gsf(HEADERS_SENT)) {
            if (0 == (state & CONTENT_LENGTH_SENT)) header(H_CONTENT_LENGTH, "0");
            if (0 == (state & DATE_SENT)) header(H_DATE, HttpProtocol.dateFormat(new Date()));
            if (0 != (state & RESP_LINE_SENT) && 0 == (state & SERVER_NAME_SENT))
                header(H_SERVER, "zorka/2.x");
            if (0 != (state & REQ_LINE_SENT) && 0 == (state & USER_AGENT_SENT))
                header(H_USER_AGENT, "zorka/2.x");
            if (0 == (state & CONNECTION_SENT)) {
                if (keepAlive()) {
                    header(H_CONNECTION, "keep-alive");
                    header(H_KEEP_ALIVE, config.getKeepAliveString());
                } else {
                    header(H_CONNECTION, "close");
                }
            }
            if (config.getHost() != null && 0 == (state & HOST_SENT)) {
                header(H_HOST, config.getHost());
            }
            write(CRLF);
            state |= HEADERS_SENT;
        }
    }

    private boolean keepAlive() {
        // TODO check if client requested Connection: close, also keepalive disabled by default for HTTP/1.0
        return !gsf(CONNECTION_CLOSE) && gsf(REQ_LINE_SENT|RESP_LINE_SENT) && config.getKeepAliveTimeout() > 0;
    }

    @Override
    public HttpMessage handle(HttpMessage m) {
        if (m.isResponse()) {
            responseLine(m.getVersion(), m.getStatus(), m.getStatusLine());
        } else {
            requestLine(m.getVersion(), m.getMethod(), uriPrefix + m.getUri(), m.getQuery());
        }

        for (Map.Entry<String, List<String>> e : m.getHeaders().entrySet()) {
            for (String s : e.getValue()) {
                header(e.getKey(), s);
            }
        }

        body(m.getBody());
        return null;
    }

}
