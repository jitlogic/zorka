package com.jitlogic.netkit;


public class ProtocolException extends NetException {

    private static final long serialVersionUID = 1L;

    private Object data = null;

    public ProtocolException(String msg, Object data) {
        super(msg);
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
