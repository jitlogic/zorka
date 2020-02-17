package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

public class NoSuchSessionException extends ZorkaRuntimeException {
    public NoSuchSessionException(String msg) {
        super(msg);
    }
}
