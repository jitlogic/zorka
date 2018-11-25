package com.jitlogic.netkit;

import com.jitlogic.netkit.NetException;

public class AbortException extends NetException {

    public AbortException(String msg) {
        super(msg);
    }

    private static final long serialVersionUID = 1L;

}
