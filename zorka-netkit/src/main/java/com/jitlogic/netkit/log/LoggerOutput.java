package com.jitlogic.netkit.log;

public interface LoggerOutput {

    void log(int level, String tag, String msg, Throwable e);

}
