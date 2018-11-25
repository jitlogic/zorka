package com.jitlogic.netkit.http;

import java.nio.channels.SelectionKey;

public interface HttpErrorHandler {

    HttpListener error(SelectionKey key, int status, String message, Object data, Throwable e);

}
