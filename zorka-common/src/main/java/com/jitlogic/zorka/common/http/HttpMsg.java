package com.jitlogic.zorka.common.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class HttpMsg {

    private Map<String, String> headers = new HashMap<String, String>();
    private byte[] body = new byte[0];

    public void setHeaders(String...hs) {
        for (int i = 1; i < hs.length; i += 2) {
            headers.put(hs[i-1], hs[i]);
        }
    }

    public Map<String,String> getHeaders() { return headers; }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public void setHeader(String name, String val) {
        headers.put(name, val);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        try {
            return new String(body, "UTF-8"); // TODO variable encodings in the future
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


}
