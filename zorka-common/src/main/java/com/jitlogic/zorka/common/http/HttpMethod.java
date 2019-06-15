/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.zorka.common.http;


public enum HttpMethod {

    GET(false),
    HEAD(false),
    POST(true),
    PUT(true),
    DELETE(true),
    TRACE(true),
    OPTIONS(true),
    CONNECT(true);

    private final boolean hasBody;
    private final byte[] bytes;

    public boolean hasBody() {
        return hasBody;
    }

    HttpMethod(boolean hasBody) {
        this.hasBody = hasBody;
        this.bytes = this.toString().getBytes();
    }

}
