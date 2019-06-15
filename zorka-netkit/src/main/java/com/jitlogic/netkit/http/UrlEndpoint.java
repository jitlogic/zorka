package com.jitlogic.netkit.http;


public interface UrlEndpoint {

    boolean matches(String vhost, String uri);

    HttpListener getListener();

}
