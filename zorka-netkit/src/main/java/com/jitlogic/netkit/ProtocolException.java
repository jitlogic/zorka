package com.jitlogic.netkit;


public class ProtocolException extends NetException {

    private Object data;

    public ProtocolException(String msg, Object data) {
        super(msg);
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
