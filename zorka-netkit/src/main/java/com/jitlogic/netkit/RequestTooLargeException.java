package com.jitlogic.netkit;

public class RequestTooLargeException extends NetException {

    private static final long serialVersionUID = 1L;

    public RequestTooLargeException(String msg) {
        super(msg);
    }
}
