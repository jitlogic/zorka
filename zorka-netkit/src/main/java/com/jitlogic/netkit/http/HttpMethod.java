package com.jitlogic.netkit.http;


import com.jitlogic.netkit.util.DynamicBytes;

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

    public DynamicBytes appendMethod(DynamicBytes dst) {
        return dst.append(bytes);
    }

    HttpMethod(boolean hasBody) {
        this.hasBody = hasBody;
        this.bytes = this.toString().getBytes();
    }

}
