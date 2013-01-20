/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Maintains zorka log filtering configuration. It allows for
 * fine tuning of which things are to be logged (useful when
 * developing/debugging more sophiscated configuration scripts.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLogConfig {

    /** Logger object */
    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaLogConfig.class);

    /** Tracer log flags: none */
    public static final int ZTR_NONE              = 0x00;

    /** Tracer configuration information */
    public static final int ZTR_CONFIG            = 0x01;

    /** Tracer-related, class-level messages from instrumentation engine. */
    public static final int ZTR_INSTRUMENT_CLASS  = 0x02;

    /** Tracer-related method-level messages from instrumentation engine. */
    public static final int ZTR_INSTRUMENT_METHOD = 0x04;

    /** Messages from symbol registry (adding and updating new symbols). */
    public static final int ZTR_SYMBOL_REGISTRY   = 0x08;

    /** Messages from symbol enricher (component that is adding names of necessary symbol names to trace streams */
    public static final int ZTR_SYMBOL_ENRICHMENT = 0x10;

    /** Tracer-related error messages */
    public static final int ZTR_TRACE_ERRORS      = 0x20;

    /** All trace calls (on beginning and end of each traced method) */
    public static final int ZTR_TRACE_CALLS       = 0x40;

    /** Tracer-related INFO log verbosity. */
    public static final int ZTR_INFO       = ZTR_CONFIG | ZTR_TRACE_ERRORS;

    /** Tracer-related DEBUG log verbosity */
    public static final int ZTR_DEBUG      = ZTR_INFO | ZTR_INSTRUMENT_CLASS | ZTR_INSTRUMENT_METHOD;

    /** Tracer-related TRACE log verbosity */
    public static final int ZTR_TRACE      = ZTR_DEBUG | ZTR_SYMBOL_REGISTRY | ZTR_SYMBOL_ENRICHMENT;

    /** Maximum verbosity (all possible tracer messages) */
    public static final int ZTR_TRACE_FULL = ZTR_TRACE | ZTR_TRACE_CALLS;

    /** Default tracer log verbosity */
    private static int tracerLevel = ZTR_INFO;

    /** Returns tracer log mask */
    public static int getTracerLevel() {
        return tracerLevel;
    }

    /**
     * Returns true if tracer is configured to log at given level
     *
     * @param level log level (bitmask)
     *
     * @return true or false
     */
    public static boolean isTracerLevel(int level) {
        return 0 != (tracerLevel & level);
    }


    /**
     * Sets tracer log mask.
     *
     * @param level tracer log level mask.
     */
    public static void setTracerLevel(int level) {
        tracerLevel = level;
    }

    /**
     * This is map of all constants defined in this class. It is used to parse
     * log verbosity parameters from zorka.properties file.
     */
    private static Map<String,Integer> flags = new HashMap<String, Integer>();


    /**
     * Parses given property string containing log verbosity information (of given subsystem).
     * String should contain of all flag (above constants with *TR_ prefix stripped) comma separated.
     *
     * For example, 'CONFIG, TRACE_ERRORS' for tracer subsystem will be equivalent of
     * ZTF_CONFIG | ZTR_TRACE_ERRORS constant (which is equal to ZTR_INFO, so 'INFO' string can be used instead).
     *
     * @param property property key (only for information purposes)
     *
     * @param prefix subsystem prefix (eg. 'ZTR_')
     *
     * @param input input string
     *
     * @return parsed integer value
     */
    public static int parse(String property, String prefix, String input) {
        int level = 0;

        for (String segment : input.split("\\,")) {
            String attr = prefix + "_" + segment.trim().toUpperCase();
            Integer flag = flags.get(attr);
            if (flag != null) {
                level |= flag;
            } else {
                log.error("Unknown config flag: " + segment + " [property: " + property + "]");
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
        tracerLevel = parse("zorka.log.tracer", "ZTR", properties.getProperty("zorka.log.tracer", "ZTR_INFO"));
    }


    static {
        for (Field field : ZorkaLogConfig.class.getFields()) {
            if (field.getName().matches("^Z[A-Z]{2}_.+")) {
                try {
                    Integer val = (Integer)field.get(ZorkaLogConfig.class);
                    flags.put(field.getName(), val);
                } catch (Exception e) {
                    log.error("Error fetching ZOrkaLogConfig." + field, e);
                }
            }
        }
    }

}
