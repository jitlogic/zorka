package com.jitlogic.zorka.common.util;

public class ZorkaConfigException extends ZorkaRuntimeException {

    public ZorkaConfigException(String msg, String val) {
        super(msg + " (val=" + val + ")");
    }

}
