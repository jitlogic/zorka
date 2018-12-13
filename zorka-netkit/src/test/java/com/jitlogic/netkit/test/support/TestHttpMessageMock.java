package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.http.HttpMessage;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestHttpMessageMock {

    private HttpMessage providedRequest;

    private HttpMessage expectedRequest;

    private HttpMessage response;

    public TestHttpMessageMock andReturn(HttpMessage resp) {
        this.response = resp;
        return this;
    }

    public HttpMessage handleRequest(HttpMessage req) {
        providedRequest = req;

        assertEquals("Request URI does not match",
                expectedRequest.getUri(), providedRequest.getUri());

        assertEquals("Query string does not match.",
                expectedRequest.getQuery(), providedRequest.getQuery());

        for (Map.Entry<String, List<String>> e : expectedRequest.getHeaders().entrySet()) {
            String name = e.getKey();

            assertEquals("Header " + name + " does not match.",
                    expectedRequest.getHeaders().get(name), providedRequest.getHeaders().get(name));
        }

        return response;
    }

    public HttpMessage getProvidedRequest() {
        return providedRequest;
    }

    public void setProvidedRequest(HttpMessage providedRequest) {
        this.providedRequest = providedRequest;
    }

    public HttpMessage getExpectedRequest() {
        return expectedRequest;
    }

    public void setExpectedRequest(HttpMessage expectedRequest) {
        this.expectedRequest = expectedRequest;
    }

    public HttpMessage getResponse() {
        return response;
    }

    public void setResponse(HttpMessage response) {
        this.response = response;
    }
}
