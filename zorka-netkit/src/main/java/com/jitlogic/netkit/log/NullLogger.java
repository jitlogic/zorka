package com.jitlogic.netkit.log;

public class NullLogger implements LoggerOutput {

    @Override
    public void log(int level, String tag, String msg, Throwable e) {
    }

}
