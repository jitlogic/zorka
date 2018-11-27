package com.jitlogic.zorka.core;

import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.log.LoggerOutput;
import org.slf4j.impl.ZorkaLogLevel;
import org.slf4j.impl.ZorkaLoggerInput;
import org.slf4j.impl.ZorkaTrapper;

import static org.slf4j.impl.ZorkaLogLevel.*;

public class NetkitTrapperLoggerOutput implements ZorkaLoggerInput, LoggerOutput {

    private volatile ZorkaTrapper trapper;

    private static final ZorkaLogLevel[] LEVELS = { FATAL, ERROR, WARN, INFO, DEBUG, TRACE };

    @Override
    public void log(int level, String tag, String msg, Throwable e) {
        trapper.trap(LEVELS[level], tag, msg, e);
    }

    @Override
    public String getName() {
        return "com.jitlogic.netkit";
    }

    @Override
    public void setLogLevel(int logLevel) {
        LoggerFactory.setLevel(5-logLevel);
    }

    @Override
    public synchronized void setTrapper(ZorkaTrapper trapper) {
        this.trapper = trapper;
    }
}
