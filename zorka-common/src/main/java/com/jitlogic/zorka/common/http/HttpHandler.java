package com.jitlogic.zorka.common.http;

public interface HttpHandler {

    HttpMessage handle(HttpMessage message);

}
