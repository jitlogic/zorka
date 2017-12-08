package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.spi.LocationAwareLogger.*;

public class ZorkaLoggerFactory implements ILoggerFactory {

    private ConcurrentMap<String, ZorkaTrapperLogger> loggerMap = new ConcurrentHashMap<String, ZorkaTrapperLogger>();
    private volatile ZorkaTrapper trapper = new MemoryTrapper();

    /** Predefined log level names */
    private static final Map<String,Integer> LOG_LEVEL_NAMES;

    /** Log levels configured for various classes and packages */
    private List<LogLevel> logLevels = new ArrayList<LogLevel>();


    private int logLevel = INFO_INT;


    /**
     * Configures log levels based on properties. All properties
     * starting with log. will be used.
     * @param props
     */
    public synchronized void configure(Properties props) {
        String defLStr = props.getProperty("log", "INFO");
        logLevel = parseLogLevel(defLStr);

        List<LogLevel> logLevels = new ArrayList<LogLevel>();

        for (String s : props.stringPropertyNames()) {
            if (s.startsWith("log.")) {
                logLevels.add(new LogLevel(s.substring(4), parseLogLevel(props.getProperty(s))));
            }
        }

        Collections.sort(logLevels,
                new Comparator<LogLevel>() {
                    @Override
                    public int compare(LogLevel o1, LogLevel o2) {
                        return o2.getPrefix().length() - o1.getPrefix().length();
                    }
                });

        this.logLevels = Collections.unmodifiableList(logLevels);

        for (Map.Entry<String,ZorkaTrapperLogger> e : loggerMap.entrySet()) {
            e.getValue().setLogLevel(logLevel(e.getValue().getName()));
        }
    }


    synchronized List<LogLevel> getLogLevels() {
        return logLevels;
    }


    public synchronized ZorkaTrapper getTrapper() {
        return trapper;
    }


    public synchronized ZorkaTrapper swapTrapper(ZorkaTrapper trapper) {

        ZorkaTrapper oldTrapper = this.trapper;

        this.trapper = trapper;

        for (Map.Entry<String,ZorkaTrapperLogger> e : loggerMap.entrySet()) {
            e.getValue().setTrapper(trapper);
        }

        if (oldTrapper instanceof MemoryTrapper) {
            for (MemoryTrapper.TrapperMessage msg : ((MemoryTrapper)oldTrapper).drain()) {
                trapper.trap(msg.getLogLevel(), msg.getTag(), msg.getMsg(), msg.getE(), msg.getArgs());
            }

        }

        return oldTrapper;
    }


    @Override
    public synchronized Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        } else {
            ZorkaTrapperLogger newInstance = new ZorkaTrapperLogger(name, logLevel(name), trapper);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }


    private int logLevel(String className) {

        for (LogLevel ll : logLevels) {
            if (className.equals(ll.getPrefix()) || className.startsWith(ll.getPrefix())) {
                return ll.getLevel();
            }
        }

        return logLevel;
    }


    private static int parseLogLevel(String lvl) {
        Integer rslt = LOG_LEVEL_NAMES.get(lvl.trim().toUpperCase());
        if (rslt != null) {
            return rslt;
        } else {
            throw new RuntimeException("Invalid log level: '" + lvl + "'");
        }
    }


    private static ZorkaLoggerFactory INSTANCE = new ZorkaLoggerFactory();


    public static ZorkaLoggerFactory getInstance() {
        return INSTANCE;
    }


    static {
        Map<String,Integer> m = new HashMap<String, Integer>();

        m.put("TRACE", TRACE_INT);
        m.put("DEBUG", DEBUG_INT);
        m.put("INFO", INFO_INT);
        m.put("WARN", WARN_INT);
        m.put("ERROR", ERROR_INT);

        LOG_LEVEL_NAMES = Collections.unmodifiableMap(m);
    }


    public static class LogLevel {
        private final String prefix;
        private final int level;
        public LogLevel(String prefix, int level) {
            this.prefix = prefix;
            this.level = level;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getLevel() {
            return level;
        }

        public String toString() {
            return prefix+"="+level;
        }
    }

}
