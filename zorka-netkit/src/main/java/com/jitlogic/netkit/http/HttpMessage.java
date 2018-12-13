package com.jitlogic.netkit.http;

import com.jitlogic.netkit.util.NetkitUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.netkit.http.HttpMethod.GET;
import static com.jitlogic.netkit.http.HttpMethod.POST;

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

    public static HttpMessage POST(String uri, Object body, String...headers) {
        HttpMessage msg = new HttpMessage(false).setUri(uri).setMethod(POST);
        msg.getBodyParts().add(body);
        hdrs(msg, headers);
        return msg;
    }

    public static HttpMessage RESP(int status) {
        return RESP(status, null);
    }

    public static HttpMessage RESP(int status, Object body, String...headers) {
        HttpMessage msg = new HttpMessage(true).setStatus(status);
        if (body != null) msg.getBodyParts().add(body);
        hdrs(msg, headers);
        return msg;
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

    public List<Object> getBodyParts() {
        return bodyParts;
    }

    public String getBodyAsString() {
        if (bodyParts.isEmpty()) {
            return null;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (Object o : bodyParts) {
                ByteBuffer bb = NetkitUtil.toByteBuffer(o);
                bos.write(bb.array(), bb.position(), bb.limit());
            }
            return new String(bos.toByteArray());
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
}
