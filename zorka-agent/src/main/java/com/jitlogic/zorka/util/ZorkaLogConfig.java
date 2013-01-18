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

package com.jitlogic.zorka.util;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ZorkaLogConfig {

    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaLogConfig.class);

    public static final int ZTR_NONE              = 0x00;
    public static final int ZTR_CONFIG            = 0x01;
    public static final int ZTR_INSTRUMENT_CLASS  = 0x02;
    public static final int ZTR_INSTRUMENT_METHOD = 0x04;
    public static final int ZTR_SYMBOL_REGISTRY   = 0x08;
    public static final int ZTR_SYMBOL_ENRICHMENT = 0x10;
    public static final int ZTR_TRACE_ERRORS      = 0x20;
    public static final int ZTR_TRACE_CALLS       = 0x40;

    public static final int ZTR_INFO       = ZTR_CONFIG | ZTR_TRACE_ERRORS;
    public static final int ZTR_DEBUG      = ZTR_INFO | ZTR_INSTRUMENT_CLASS | ZTR_INSTRUMENT_METHOD;
    public static final int ZTR_TRACE      = ZTR_DEBUG | ZTR_SYMBOL_REGISTRY | ZTR_SYMBOL_ENRICHMENT;
    public static final int ZTR_TRACE_FULL = ZTR_TRACE | ZTR_TRACE_CALLS;

    private static int tracerLevel = ZTR_NONE;


    public static int getTracerLevel() {
        return tracerLevel;
    }


    public static boolean isTracerLevel(int level) {
        return 0 != (tracerLevel & level);
    }

    public static void setTracerLevel(int level) {
        tracerLevel = level;
    }

    private static Map<String,Integer> flags = new HashMap<String, Integer>();


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
