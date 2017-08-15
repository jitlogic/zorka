/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;

import java.util.Map;

import static org.junit.Assert.*;

public class TestHttpRequestMock {

    private HttpRequest providedRequest;

    private HttpRequest expectedRequest;

    private HttpResponse response;


    public TestHttpRequestMock andReturn(HttpResponse resp) {
        this.response = resp;
        return this;
    }


    public HttpResponse handleRequest(HttpRequest req) {
        providedRequest = req;

        assertEquals("Request URL does not match",
            expectedRequest.url(), providedRequest.url());

        for (Map.Entry<String,String> e : expectedRequest.params().entrySet()) {
            String name = e.getKey();
            assertEquals("Parameter " + name + " does not match.",
                expectedRequest.param(name), providedRequest.param(name));
        }

        for (Map.Entry<String,String> e : expectedRequest.headers().entrySet()) {
            String name = e.getKey();
            assertEquals("Header " + name + " + does not match.",
                expectedRequest.header(name), providedRequest.header(name));
        }

        return response;
    }


    public HttpRequest getProvidedRequest() {
        return providedRequest;
    }


    public void setProvidedRequest(HttpRequest providedRequest) {
        this.providedRequest = providedRequest;
    }


    public HttpRequest getExpectedRequest() {
        return expectedRequest;
    }


    public void setExpectedRequest(HttpRequest expectedRequest) {
        this.expectedRequest = expectedRequest;
    }


    public HttpResponse getResponse() {
        return response;
    }


    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public void response(int status, String body) {
        setResponse(RESP(status, body));
    }

    public static HttpResponse RESP(int status, String body) {
        return new HttpResponse().setResponseCode(status).body(body);
    }

}
