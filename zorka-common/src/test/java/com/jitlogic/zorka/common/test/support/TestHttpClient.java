/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.http.HttpClient;
import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestHttpClient implements HttpClient {

    private List<TestHttpRequestMock> mocks = new ArrayList<TestHttpRequestMock>();
    private List<HttpRequest> requests = new ArrayList<HttpRequest>();
    private List<HttpResponse> responses = new ArrayList<HttpResponse>();

    private int curIdx = 0;

    @Override
    public HttpResponse execute(HttpRequest req) throws IOException {
        if (curIdx < mocks.size()) {
            requests.add(req);
            HttpResponse resp = mocks.get(curIdx++).handleRequest(req);
            responses.add(resp);
            return resp;
        } else {
            fail("Unexpected HTTP call.");
        }
        return null;
    }

    public TestHttpRequestMock expect(HttpRequest expectedReq) {
        TestHttpRequestMock mock = new TestHttpRequestMock();
        mock.setExpectedRequest(expectedReq);
        mocks.add(mock);
        return mock;
    }

    public int numRequests() {
        return requests.size();
    }

    public HttpRequest getRequest(int idx) {
        return requests.get(idx);
    }

    public HttpResponse getResponse(int idx) {
        return responses.get(idx);
    }

    public void verify() {
        if (curIdx < mocks.size()) {
            fail("Not all requests have been executed.");
        }
    }
}
