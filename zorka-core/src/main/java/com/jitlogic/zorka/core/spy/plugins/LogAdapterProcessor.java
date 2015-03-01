/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLogLevel;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Extracts interesting values from log records of various types (JDK, Log4j, JBoss etc.).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LogAdapterProcessor implements SpyProcessor {

    /**
     * Log level field suffix
     */
    public static final String TAG_LEVEL = "_LEVEL";

    /**
     * Source class field suffix
     */
    public static final String TAG_CLASS = "_CLASS";

    /**
     * Source method field suffix
     */
    public static final String TAG_METHOD = "_METHOD";

    /**
     * Log message field suffix
     */
    public static final String TAG_MESSAGE = "_MESSAGE";

    /**
     * Exception object
     */
    public static final String TAG_EXCEPTION = "_EXCEPTION";

    /**
     * Spy record field containing log record
     */
    private String src;

    /**
     * Prefix for output fields
     */
    private String prefix;

    /**
     * Standard constructor.
     *
     * @param src    field containing log record
     * @param prefix prefix for output fields
     */
    public LogAdapterProcessor(String src, String prefix) {
        this.src = src;
        this.prefix = prefix;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object orig = record.get(src);

        if (orig != null && "java.util.logging.LogRecord".equals(orig.getClass().getName())) {
            adaptJdkRecord(orig, record);
        }

        return record;
    }

    /**
     * Thresholds for mapping JDK log levels to ZorkaLogLevel
     */
    private static int[] jdkThresholds = {
            300,                  // FINEST
            500,                  // FINER, FINE
            800,                  // CONFIG, INFO
            900,                  // WARNING
            1000,                 // SEVERE
    };


    /**
     * JDK to Zorka log level thresholds map
     */
    private static ZorkaLogLevel[] jdkLevels = {
            ZorkaLogLevel.TRACE,  // FINEST
            ZorkaLogLevel.DEBUG,  // FINER, FINE
            ZorkaLogLevel.INFO,   // CONFIG, INFO
            ZorkaLogLevel.WARN,   // WARNING
            ZorkaLogLevel.ERROR,  // SEVERE
            ZorkaLogLevel.FATAL   // everything above
    };


    /**
     * Extracts data from Zorka log records
     *
     * @param orig JDK log record
     * @param rec  spy record to be populated
     */
    private void adaptJdkRecord(Object orig, Map<String, Object> rec) {
        Integer level = ObjectInspector.get(orig, "level", "intValue()");

        ZorkaLogLevel logLevel = null;

        for (int i = 0; i < jdkThresholds.length; i++) {
            logLevel = jdkLevels[i];
            if (level <= jdkThresholds[i]) {
                break;
            }
        }

        if (logLevel == null) {
            logLevel = ZorkaLogLevel.FATAL;
        }

        rec.put(prefix + TAG_LEVEL, logLevel);
        rec.put(prefix + TAG_CLASS, ObjectInspector.get(orig, "sourceClassName"));
        rec.put(prefix + TAG_METHOD, ObjectInspector.get(orig, "sourceMethodName"));
        rec.put(prefix + TAG_MESSAGE, ObjectInspector.get(orig, "message"));
        rec.put(prefix + TAG_EXCEPTION, ObjectInspector.get(orig, "thrown"));
    }
}
