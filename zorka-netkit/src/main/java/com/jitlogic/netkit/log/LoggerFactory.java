package com.jitlogic.netkit.log;


import com.jitlogic.netkit.NetException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static singleton factory object for loggers and event sinks (performance counters).
 */
public class LoggerFactory {

    public static final int TRACE_LEVEL = 5;
    public static final int DEBUG_LEVEL = 4;
    public static final int INFO_LEVEL  = 3;
    public static final int WARN_LEVEL  = 2;
    public static final int ERROR_LEVEL = 1;
    public static final int FATAL_LEVEL = 0;

    static final List<String> LEVELS = Collections.unmodifiableList(
            Arrays.asList("FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"));

    private static volatile int level = INFO_LEVEL;
    private static volatile LoggerOutput output = new ConsoleLogger(INFO_LEVEL, System.err);

    private static final ConcurrentMap<String,Logger> loggers = new ConcurrentHashMap<String, Logger>();

    private static volatile Class<? extends EventSink> defaultSinkImpl = CountingSink.class;
    private static volatile Object[] defaultSinkExtraArgs = new Object[0];
    private static final ConcurrentMap<String,EventSink> sinks = new ConcurrentHashMap<String, EventSink>();

    public static final String EV_ACCEPTS = "httpkit.server.accept";
    public static final String EV_LOOPS = "httpkit.server.loop";
    public static final String EV_STATUS = "httpkit.server.status.";
    public static final String EV_HTTP_DECODE = "httpkit.server.http.decode";
    public static final String EV_HTTP_DROPS = "httpkit.server.http.drops";
    public static final String EV_ILLEGAL_OPS = "httpkit.engine.illegal.ops";

    public static final String EV_WS_DECODE = "httpkit.server.ws.decode";
    public static final String EV_WS_FRAMES = "http.server.ws.frame";

    public static final String EV_CLI_IMPOSSIBLE = "httpkit.client.impossible";
    public static final String EV_CH_CLOSE = "httpkit.server.channel.close";

    private LoggerFactory() {
        // Cannot instantiate
    }

    static int getLevel() {
        return level;
    }

    static void log(int level, String tag, String msg, Throwable e) {
        output.log(level, tag, msg, e);
    }

    public static Logger getLogger(Class<?> clazz) {
        String name = clazz != null ? clazz.getName() : "?";
        Logger logger = loggers.get(name);
        if (logger == null) {
            synchronized (loggers) {
                logger = new Logger(name);
                loggers.put(name, logger);
            }
        }
        return logger;
    }

    public static synchronized void setLevel(int level) {
        LoggerFactory.level = level;
    }

    public static synchronized void setOutput(LoggerOutput output) {
        LoggerFactory.output = output;
    }

    public static EventSink getSink(String name) {
        EventSink sink = sinks.get(name);
        if (sink == null) {
            synchronized (sinks) {
                try {
                    Object[] args = new Object[defaultSinkExtraArgs.length+1];
                    args[0] = name;
                    if (defaultSinkExtraArgs.length > 0)
                        System.arraycopy(defaultSinkExtraArgs, 0, args, 1, defaultSinkExtraArgs.length);
                    sink = (EventSink)defaultSinkImpl.getConstructors()[0].newInstance(args);
                    sinks.put(name, sink);
                } catch (Exception e) {
                    throw new NetException("Error constructing sink object", e);
                }
            }
        }
        return sink;
    }

    public static synchronized void setDefaultSink(Class<? extends EventSink> sinkImpl, Object...extraArgs) {
        synchronized (sinks) {
            sinks.clear();
            defaultSinkImpl = sinkImpl;
            defaultSinkExtraArgs = extraArgs;
        }
    }

}
