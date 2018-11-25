package com.jitlogic.netkit.integ.ring;

import clojure.lang.Keyword;

import static clojure.lang.Keyword.intern;

public class RingProtocol {
    public static final Keyword SERVER_PORT = intern("server-port");
    public static final Keyword SERVER_NAME = intern("server-name");
    public static final Keyword REMOTE_ADDR = intern("remote-addr");
    public static final Keyword PROTOCOL = intern("protocol");
    public static final Keyword URI = intern("uri");
    public static final Keyword QUERY_STRING = intern("query-string");
    public static final Keyword SCHEME = intern("scheme");
    public static final Keyword REQUEST_METHOD = intern("request-method");
    public static final Keyword REQ_HEADERS = intern("headers");
    public static final Keyword CONTENT_TYPE = intern("content-type");
    public static final Keyword CONTENT_LENGTH = intern("content-length");
    public static final Keyword CHARACTER_ENCODING = intern("character-encoding");
    public static final Keyword BODY_DATA = intern("body");
    public static final Keyword WEBSOCKET = intern("websocket?");
    public static final Keyword ASYC_CHANNEL = intern("async-channel");
    public static final Keyword SSL_CLIENT_CERT = intern("ssl-client-cert");


    public static final Keyword HTTP = intern("http");
    public static final Keyword HTTPS = intern("https");
    public static final Keyword STATUS = intern("status");

    public static final Keyword HTTP_GET = intern("get");
    public static final Keyword HTTP_POST = intern("post");

}
