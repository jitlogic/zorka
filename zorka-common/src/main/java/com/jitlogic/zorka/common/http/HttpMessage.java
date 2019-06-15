package com.jitlogic.zorka.common.http;

import java.util.*;

import static com.jitlogic.zorka.common.http.HttpMethod.GET;
import static com.jitlogic.zorka.common.http.HttpMethod.POST;

/**
 * HTTP message represents either request or reply. This class along with
 * HttpMessageHandler and HttpMessageListener can be used when programmer
 * prefers convenience over performance.
 */
public class HttpMessage {

    private static void hdrs(HttpMessage msg, String[] headers) {
        for (int i = 1; i < headers.length; i += 2) {
            msg.header(headers[i - 1], headers[i]);
        }
    }

    public static HttpMessage GET(String uri, String...headers) {
        HttpMessage msg = new HttpMessage(false).setUri(uri).setMethod(GET);
        hdrs(msg, headers);
        return msg;
    }

    public static HttpMessage POST(String uri, String body, String...headers) {
        HttpMessage msg = new HttpMessage(false).setUri(uri).setMethod(POST);
        if (body != null) msg.body = body.getBytes();
        hdrs(msg, headers);
        return msg;
    }

    public static HttpMessage POST(String uri, byte[] body, String...headers) {
        HttpMessage msg = new HttpMessage(false).setUri(uri).setMethod(POST);
        msg.body = body;
        hdrs(msg, headers);
        return msg;
    }

    public static HttpMessage RESP(int status, String body, String...headers) {
        HttpMessage msg = new HttpMessage(true).setStatus(status);
        if (body != null) msg.body = body.getBytes();
        hdrs(msg, headers);
        return msg;
    }

    public static HttpMessage RESP(int status, byte[] body, String...headers) {
        HttpMessage msg = new HttpMessage(true).setStatus(status);
        msg.body = body;
        hdrs(msg, headers);
        return msg;
    }

    private boolean isResponse;

    private String version = HttpProtocol.HTTP_1_1;

    private HttpMethod method;

    private int status;

    private String uri;

    private String query;

    private String StatusLine;

    private Map<String,List<String>> headers = new TreeMap<String, List<String>>();

    private byte[] body;

    public HttpMessage(boolean isResponse) {
        this.isResponse = isResponse;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public HttpMessage setResponse(boolean response) {
        this.isResponse = response;
        return this;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public HttpMessage setMethod(HttpMethod method) {
        this.method = method;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public HttpMessage setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public HttpMessage setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public HttpMessage header(String key, String val) {
        if (!headers.containsKey(key)) headers.put(key, new ArrayList<String>());
        headers.get(key).add(val);
        return this;
    }

    public String getHeader(String key) {
        return headers.containsKey(key) ? headers.get(key).get(0) : null;
    }

    public HttpMessage headers(String...keyvals) {
        hdrs(HttpMessage.this, keyvals);
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getBodyAsString() {
        if (body == null) {
            return null;
        } else {
            return new String(body);
        }
    }

    public String getVersion() {
        return version;
    }

    public HttpMessage setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public HttpMessage setQuery(String query) {
        this.query = query;
        return this;
    }

    public String getStatusLine() {
        return StatusLine;
    }

    public void setStatusLine(String statusLine) {
        StatusLine = statusLine;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
