package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.http.HttpListener;
import com.jitlogic.netkit.http.HttpMethod;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestHttpListener implements HttpListener {

    public static final Object SKIP = new Object();

    private List<Object[]> calls = new ArrayList<Object[]>();

    public int count(String method) {
        int rslt = 0;

        for (Object[] s : calls) {
            if (method.equals(s[0])) rslt++;
        }

        return rslt;
    }

    public void check(String method, Object...args) {
        for (Object[] call : calls) {
            if (method.equals(call[0])) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] != SKIP) {
                        assertEquals("Method " + method + ": arg" + i, args[i], call[i+1]);
                    }
                }
                return;
            }
        }
        fail("Method call " + method + " not found.");
    }

    public String getBody() {
        StringBuilder sb = new StringBuilder();
        for (Object[] call : calls) {
            if ("body".equals(call[0])) {
                for (Object o : (Object[])call[2]) {
                    ByteBuffer bb = (ByteBuffer)o;
                    if (bb.hasRemaining()) {
                        byte[] b = new byte[bb.remaining()];
                        bb.get(b);
                        sb.append(new String(b));
                    }
                }
            }
        }
        return sb.toString();
    }

    public TestHttpListener request(SelectionKey key, String httpVersion, HttpMethod method, String url, String query) {
        calls.add(new Object[]{"request", key, httpVersion, method, url, query});
        return this;
    }

    public TestHttpListener response(SelectionKey key, String httpVersion, int status, String statusMessage) {
        calls.add(new Object[]{"response", key, httpVersion, status, statusMessage});
        return this;
    }

    public TestHttpListener header(SelectionKey key, String name, String value) {
        calls.add(new Object[]{"header", key, name, value});
        return this;
    }

    public TestHttpListener body(SelectionKey key, Object...parts) {
        calls.add(new Object[]{"body", key, parts});
        return this;
    }

    public TestHttpListener finish(SelectionKey key) {
        calls.add(new Object[]{"finish", key});
        return this;
    }

    public TestHttpListener error(SelectionKey key, int status, String message, Object data, Throwable e) {
        calls.add(new Object[]{"error", key, status, message, data, e});
        return this;
    }
}
