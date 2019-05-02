/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.slf4j.spi.LocationAwareLogger.*;

public class ZorkaLoggerFactory implements ILoggerFactory {

    private ConcurrentMap<String, ZorkaTrapperLogger> loggerMap = new ConcurrentHashMap<String, ZorkaTrapperLogger>();
    private ZorkaTrapper trapper = new MemoryTrapper();

    /** Additional inputs (from external APIs integrations, eg. netkit). */
    private List<ZorkaLoggerInput> inputs = new CopyOnWriteArrayList<ZorkaLoggerInput>();

    /** Predefined log level names */
    private static final Map<String,Integer> LOG_LEVEL_NAMES;

    /** Log levels configured for various classes and packages */
    private List<LogLevel> logLevels = new ArrayList<LogLevel>();

    /** Default log level. */
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

        this.logLevels = logLevels;

        for (Map.Entry<String,ZorkaTrapperLogger> e : loggerMap.entrySet()) {
            e.getValue().setLogLevel(logLevel(e.getValue().getName()));
        }
    }

    public void shutdown() {
        if (trapper instanceof MemoryTrapper) {
            ((MemoryTrapper) trapper).drain();
        } else {
            trapper = new MemoryTrapper();
            // TODO trapper.shutdown() here
        }
        loggerMap.clear();
        logLevel = INFO_INT;
    }

    public synchronized List<LogLevel> getLogLevels() {
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

    public synchronized void swapInput(ZorkaLoggerInput input) {

        input.setLogLevel(5-logLevel);
        input.setTrapper(trapper);

        boolean found = false;

        for (int i = 0; i < inputs.size(); i++) {
            ZorkaLoggerInput inp = inputs.get(i);
            if (input.getName().equals(inp.getName())) {
                inputs.set(i, input);
                found = true;
                break;
            }
        }

        if (!found) {
            inputs.add(input);
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
