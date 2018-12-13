package com.jitlogic.netkit.test.support;

import com.jitlogic.netkit.http.HttpMessage;
import com.jitlogic.netkit.http.HttpMessageClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TestHttpMessageClient implements HttpMessageClient {

    private List<TestHttpMessageMock> mocks = new ArrayList<TestHttpMessageMock>();
    private List<HttpMessage> requests = new ArrayList<HttpMessage>();
    private List<HttpMessage> responses = new ArrayList<HttpMessage>();

    private int curIdx = 0;

    @Override
    public HttpMessage exec(HttpMessage req) {
        if (curIdx < mocks.size()) {
            requests.add(req);
            HttpMessage resp = mocks.get(curIdx++).handleRequest(req);
            responses.add(resp);
            return resp;
        } else {
            fail("Unexpected HTTP call.");
        }
        return null;
    }

    public TestHttpMessageMock expect(HttpMessage expectedReq) {
        TestHttpMessageMock mock = new TestHttpMessageMock();
        mock.setExpectedRequest(expectedReq);
        mocks.add(mock);
        return mock;
    }

    public int numRequests() {
        return requests.size();
    }

    public HttpMessage getRequest(int idx) {
        return requests.get(idx);
    }

    public HttpMessage getResponse(int idx) {
        return responses.get(idx);
    }

    public void verify() {
        if (curIdx < mocks.size()) {
            fail("Not all requests have been executed.");
        }
    }

}
