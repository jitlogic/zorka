package com.jitlogic.netkit.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP message represents either request or reply. This class along with
 * HttpMessageHandler and HttpMessageListener can be used when programmer
 * prefers convenience over performance.
 */
public class HttpMessage {

    public static HttpMessage RESP(int status) {
        return new HttpMessage(true).setStatus(status);
    }

    private boolean isResponse;

    private String version = HttpProtocol.HTTP_1_1;

    private HttpMethod method;

    private int status;

    private String uri;

    private String query;

    private Map<String,List<String>> headers = new HashMap<String, List<String>>();

    private List<Object> bodyParts = new ArrayList<Object>();

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

    public void setMethod(HttpMethod method) {
        this.method = method;
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

    public HttpMessage headers(String...keyvals) {
        for (int i = 1; i < keyvals.length; i += 2) {
            header(keyvals[i-1], keyvals[i]);
        }
        return this;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public List<Object> getBodyParts() {
        return bodyParts;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
