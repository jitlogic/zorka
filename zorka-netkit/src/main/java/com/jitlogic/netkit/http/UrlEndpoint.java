package com.jitlogic.netkit.http;

import com.jitlogic.netkit.http.HttpListener;

public interface UrlEndpoint {

    boolean matches(String vhost, String uri);

    HttpListener getListener();

}
