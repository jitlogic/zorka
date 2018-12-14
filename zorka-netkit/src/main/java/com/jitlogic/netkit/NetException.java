/* Derived from httpkit (http://http-kit.org) under Apache License. See LICENSE.txt for more details.  */

package com.jitlogic.netkit;

public class NetException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NetException(String msg) {
        super(msg);
    }

    public NetException(String msg, Exception e) {
        super(msg, e);
    }
}
