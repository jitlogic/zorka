/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.zorka.common.http;

import java.io.InputStream;
import java.util.regex.Matcher;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jitlogic.zorka.common.http.HttpProtocol.*;

public class HttpDecoder {

    private Logger log = LoggerFactory.getLogger(HttpDecoder.class);

    public final static String MALFORMED_MSG = "malformed HTTP message";
    public final static String TOO_LARGE_MSG = "too large";

    public static final int BODY_MAX = 8 * 1024 * 1024;
    public static final int HEADERS_MAX = 64 * 1024;
    public static final int LINE_MAX = 32 * 1024;

    private final int maxBody;
    private final int maxLine;

    private HttpMessage httpMessage;

    private int contentLength = 0;
    boolean chunked = false;

    private HttpConfig config;
    private BufferedStreamLineReader reader;

    public HttpDecoder(InputStream is, HttpConfig config) {
        this.reader = new BufferedStreamLineReader(is, config.getMaxLineSize());
        this.config = config;
        this.maxLine = config.getMaxLineSize();
        this.maxBody = config.getMaxBodySize();
    }

    private void parseResponseLine() {
        String line = reader.readLine();
        Matcher m = RE_RESP_LINE.matcher(line);
        if (m.matches()) {
            httpMessage.setVersion(m.group(1));
            httpMessage.setStatus(Integer.parseInt(m.group(2)));
            httpMessage.setStatusLine(m.group(3));
        } else {
            throw new HttpException("Error decoding HTTP message", 400, MALFORMED_MSG, line, null);
        }
    }

    private void parseRequestLine() {
        String line = reader.readLine();
        if (line.isEmpty()) throw new HttpClosedException();
        Matcher m = RE_REQ_LINE.matcher(line);
        if (m.matches()) {
            httpMessage.setMethod(HttpMethod.valueOf(m.group(1)));
            httpMessage.setUri(m.group(2));
            httpMessage.setQuery(m.group(3));
            httpMessage.setVersion(m.group(4));
        } else {
            throw new HttpException("Error decoding HTTP message", 400, MALFORMED_MSG, line, null);
        }
    }

    private void processHeader(String k, String v) {
        if (v.indexOf(',') != -1) {
            for (String s : v.split(",")) {
                httpMessage.header(k, s);
            }
        } else {
            httpMessage.header(k, v);
        }
    }

    private int processContentLength(String cl) {
        int rr = 0;
        if (cl != null) {
            try {
                rr = Integer.parseInt(cl);
                if (rr > 0) {
                    if (rr > maxBody) {
                        throw new HttpException("Error decoding HTTP message", 400, TOO_LARGE_MSG, cl, null);
                    }
                }
            } catch (NumberFormatException e) {
                throw new HttpException("Error decoding HTTP message", 400, MALFORMED_MSG, "Content-Length: " + cl, e);
            }
        }
        return rr;
    }

    private void parseHeaders() {
        String line = reader.readLine();
        String cl = null;

        while (line != null && !line.isEmpty()) {
            Matcher m = RE_HEADER.matcher(line);
            if (m.matches()) {
                String k = m.group(1), v = m.group(2);
                processHeader(k, v);
                chunked |= H_TRANSFER_ENCODING.equalsIgnoreCase(k) && "chunked".equalsIgnoreCase(v);
                if (H_CONTENT_LENGTH.equalsIgnoreCase(k)) {
                    cl = v;
                }
            } else {
                throw new HttpException("Error parsing HTTP header", 400, MALFORMED_MSG, line, null);
            }
            line = reader.readLine();
        }
        this.contentLength = chunked ? 0 : processContentLength(cl);
    }

    public HttpMessage decode(boolean isResponse) {
        chunked = false;
        contentLength = 0;
        httpMessage = new HttpMessage(isResponse);

        if (!isResponse) {
            parseRequestLine();
        } else {
            parseResponseLine();
        }

        parseHeaders();

        if (chunked) {
            int readCount = 0;
            for (int rr = getChunkSize(reader.readLine()); rr > 0; rr = getChunkSize(reader.readLine())) {
                if (readCount + rr > maxBody) {
                    throw new HttpException("Error decoding HTTP message", 400, "", TOO_LARGE_MSG, null);
                }
                byte[] c = httpMessage.getBody() == null ? new byte[rr]
                    : ZorkaUtil.clipArray(httpMessage.getBody(), httpMessage.getBody().length+rr);
                byte[] x = reader.readData(rr);
                System.arraycopy(x, 0, c, c.length-rr, rr);
                httpMessage.setBody(c);
                reader.readLine(); // Read empty line
            }
        } else if (contentLength > 0 && httpMessage.getStatus() != 204) {
            // TODO handle chunked encoding
            httpMessage.setBody(reader.readData(contentLength));
        }

        return httpMessage;
    }

    public void reset() {
        httpMessage = null;
    }

    private int getChunkSize(String hex) {
        // TODO switch to regex
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            throw new HttpException("Error decoding HTTP message", 400, "", MALFORMED_MSG, e);
        }
    }
}
