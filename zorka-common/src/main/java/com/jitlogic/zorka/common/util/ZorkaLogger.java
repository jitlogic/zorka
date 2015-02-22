/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.common.util;

import java.lang.reflect.Field;
import java.util.*;

/**
 * This has been written from scratch in order to not interfere with
 * other logging frameworks.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLogger implements ZorkaTrapper {

    // TODO ensure overall modeling for logging is done properly: subsystems vs priorities

    /**
     * Agent: Tracer log flags
     */
    public static final long ZTR_NONE = 0x00;
    public static final long ZTR_CONFIG = 0x01;
    public static final long ZTR_INSTRUMENT_CLASS = 0x02;
    public static final long ZTR_INSTRUMENT_METHOD = 0x04;
    public static final long ZTR_SYMBOL_REGISTRY = 0x08;
    public static final long ZTR_SYMBOL_ENRICHMENT = 0x10;
    public static final long ZTR_TRACE_ERRORS = 0x20;
    public static final long ZTR_TRACE_CALLS = 0x40;
    public static final long ZTR_TRACE_EXCEPTIONS = 0x4000;
    public static final long ZTR_TRACER_DBG = 0x8000;
    public static final long ZTR_ERRORS = 0x80;
    public static final long ZTR_INFO = ZTR_CONFIG | ZTR_TRACE_ERRORS;
    public static final long ZTR_DEBUG = ZTR_INFO | ZTR_INSTRUMENT_CLASS | ZTR_INSTRUMENT_METHOD | ZTR_SYMBOL_REGISTRY | ZTR_SYMBOL_ENRICHMENT | ZTR_TRACER_DBG;
    public static final long ZTR_TRACE = ZTR_DEBUG | ZTR_TRACE_CALLS | ZTR_TRACE_EXCEPTIONS;

    /**
     * Agent: Performance metrics log flags
     */
    public static final long ZPM_NONE = 0x0000;
    public static final long ZPM_CONFIG = 0x0100;
    public static final long ZPM_RUNS = 0x0200;
    public static final long ZPM_RUN_DEBUG = 0x0400;
    public static final long ZPM_RUN_TRACE = 0x0800;
    public static final long ZPM_ERRORS = 0x1000;
    public static final long ZPM_INFO = ZPM_CONFIG | ZPM_ERRORS;
    public static final long ZPM_DEBUG = ZPM_INFO | ZPM_RUNS | ZPM_RUN_DEBUG;
    public static final long ZPM_TRACE = ZPM_DEBUG | ZPM_RUN_TRACE;

    /**
     * Agent: Zorka Spy log flags
     */
    public static final long ZSP_NONE = 0x000000;
    public static final long ZSP_CONFIG = 0x010000;
    public static final long ZSP_CLASS_DBG = 0x020000;
    public static final long ZSP_METHOD_DBG = 0x040000;
    public static final long ZSP_SUBMIT = 0x080000;
    public static final long ZSP_ERRORS = 0x100000;
    public static final long ZSP_CLASS_TRC = 0x200000;
    public static final long ZSP_METHOD_TRC = 0x400000;
    public static final long ZSP_ARGPROC = 0x800000;
    public static final long ZSP_INFO = ZSP_CONFIG | ZSP_ERRORS;
    public static final long ZSP_DEBUG = ZSP_INFO | ZSP_CLASS_DBG | ZSP_METHOD_DBG;
    public static final long ZSP_TRACE = ZSP_DEBUG | ZSP_CLASS_TRC | ZSP_SUBMIT | ZSP_ARGPROC | ZSP_METHOD_TRC;


    /**
     * Agent: Core log flags
     */
    public static final long ZAG_NONE = 0x00000000;
    public static final long ZAG_CONFIG = 0x01000000;
    public static final long ZAG_QUERIES = 0x02000000;
    public static final long ZAG_WARNINGS = 0x04000000;
    public static final long ZAG_TRACES = 0x08000000;
    public static final long ZAG_ERRORS = 0x10000000;
    public static final long ZAG_INFO = ZAG_CONFIG | ZAG_ERRORS;
    public static final long ZAG_DEBUG = ZAG_INFO | ZAG_QUERIES;
    public static final long ZAG_TRACE = ZAG_DEBUG | ZAG_TRACES;

    /**
     * Agent/Zico: Collector log flags
     */
    public static final long ZCL_NONE = 0x0000000000L;
    public static final long ZCL_CONFIG = 0x0100000000L;
    public static final long ZCL_STORE = 0x0200000000L;
    public static final long ZCL_ZICO = 0x0400000000L;
    public static final long ZCL_ERRORS = 0x4000000000L;
    public static final long ZCL_INFO = ZCL_CONFIG | ZCL_STORE | ZCL_ERRORS;
    public static final long ZCL_DEBUG = ZCL_INFO;
    public static final long ZCL_TRACE = ZCL_INFO;

    /**
     * Default perfmon log verbosity
     */
    private static long logMask = ZTR_INFO | ZPM_INFO | ZSP_INFO | ZAG_INFO | ZCL_INFO;

    /**
     * Returns true if tracer is configured to log at given mask
     *
     * @param mask log mask (bitmask)
     * @return true or false
     */
    public static boolean isLogMask(long mask) {
        return 0 != (logMask & mask);
    }


    /**
     * Sets tracer log mask.
     *
     * @param level tracer log level mask.
     */
    public static void setLogMask(long level) {
        logMask = level;
    }

    /**
     * This is map of all constants defined in this class. It is used to parse
     * log verbosity parameters from zorka.properties file.
     */
    private static Map<String, Long> flags = new HashMap<String, Long>();


    private static ZorkaLogLevel logLevel = ZorkaLogLevel.INFO;


    /**
     * Parses given property string containing log verbosity information (of given subsystem).
     * String should contain of all flag (above constants with *TR_ prefix stripped) comma separated.
     * <p/>
     * For example, 'CONFIG, TRACE_ERRORS' for tracer subsystem will be equivalent of
     * ZTF_CONFIG | ZTR_TRACE_ERRORS constant (which is equal to ZTR_INFO, so 'INFO' string can be used instead).
     *
     * @param property property key (only for information purposes)
     * @param prefix   subsystem prefix (eg. 'ZTR_')
     * @param input    input string
     * @return parsed integer value
     */
    public static long parse(String property, String prefix, String input) {
        long level = 0;

        for (String segment : input.split("\\,")) {
            String attr = prefix + "_" + segment.trim().toUpperCase();
            Long flag = flags.get(attr);
            if (flag != null) {
                level |= flag;
            } else {
                System.err.println("ZorkaLogger: Unknown config flag: " + segment + " [property: " + property + "]");
            }
        }

        return level;
    }


    /**
     * Configures log filtering configuration from given properties object.
     *
     * @param properties configuration properties (read from zorka.properties file).
     */
    public static void configure(Properties properties) {
        logMask = parse("zorka.log.tracer", "ZTR", properties.getProperty("zorka.log.tracer", "INFO"))
                | parse("zorka.log.perfmon", "ZPM", properties.getProperty("zorka.log.perfmon", "INFO"))
                | parse("zorka.log.spy", "ZSP", properties.getProperty("zorka.log.spy", "INFO"))
                | parse("zorka.log.agent", "ZAG", properties.getProperty("zorka.log.agent", "INFO"))
                | parse("zorka.log.collect", "ZCL", properties.getProperty("zorka.log.collect", "INFO"));
        logLevel = ZorkaLogLevel.valueOf(properties.getProperty("zorka.log.level", "INFO").trim().toUpperCase());
    }


    static {
        for (Field field : ZorkaLogger.class.getFields()) {
            if (field.getName().matches("^Z[A-Z]{2}_.+")) {
                try {
                    Long val = (Long) field.get(ZorkaLogger.class);
                    flags.put(field.getName(), val);
                } catch (Exception e) {
                    System.err.println("ZorkaLogger: Error fetching log config: " + field + "   " + e.getMessage());
                }
            }
        }
    }

    /**
     * Logger
     */
    private static ZorkaLogger logger;


    /**
     * Returns client-side logger object
     *
     * @param clazz source class
     * @return ZorkaLog object
     */
    public static ZorkaLog getLog(Class<?> clazz) {
        String[] segs = clazz.getName().split("\\.");
        return getLog(segs[segs.length - 1]);
    }


    /**
     * Returns client-side logger object
     *
     * @param tag log tag
     * @return ZorkaLog object
     */
    public static ZorkaLog getLog(String tag) {
        return new ZorkaLog(tag, getLogger());
    }


    /**
     * Returns logger instance
     *
     * @return logger
     */
    public static synchronized ZorkaLogger getLogger() {
        if (logger == null) {
            logger = new ZorkaLogger();
        }
        return logger;
    }


    /**
     * Sets logger instance.
     * TODO remove this method, it is only used by unit tests
     *
     * @param newLogger new logger
     */
    public static void setLogger(ZorkaLogger newLogger) {
        logger = newLogger;
    }


    /**
     * List of trappers that will receive log messages
     */
    private List<ZorkaTrapper> trappers = new ArrayList<ZorkaTrapper>();


    /**
     * Limits instantiations of this singleton class
     */
    protected ZorkaLogger() {
    }


    /**
     * Adds new trapper to this logger.
     *
     * @param trapper trapper
     */
    public synchronized void addTrapper(ZorkaTrapper trapper) {
        trappers.add(trapper);
    }


    public synchronized void shutdown() {
        for (ZorkaTrapper trapper : trappers) {
            if (trapper instanceof ZorkaAsyncThread) {
                ((ZorkaAsyncThread) trapper).stop();
            }
        }
        trappers.clear();
    }


    /**
     * Logs a message. Log message is sent to all registered trappers.
     *
     * @param level    log level
     * @param tag      log message tag (eg. component name)
     * @param message  message text (optionally format string)
     * @param e        exception thrown (if any)
     * @param args     optional argument used when message text is a format string
     */
    public synchronized void trap(ZorkaLogLevel level, String tag, String message, Throwable e, Object... args) {
        if (level.getPriority() >= logLevel.getPriority()) {
            for (ZorkaTrapper trapper : trappers) {
                trapper.trap(logLevel, tag, message, e, args);
            }
        }
    }

}
