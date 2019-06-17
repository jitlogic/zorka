package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.http.HttpMessage;

public class TestHttpClient implements HttpHandler {
    @Override
    public HttpMessage handle(HttpMessage message) {
        return HttpMessage.RESP(200, "OK");
    }
}
