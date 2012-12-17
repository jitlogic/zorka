/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.logproc;

import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLogLevel;

public class LogAdapter {

    private ObjectInspector inspector = new ObjectInspector();

    private static int[] jdkThresholds = {
            300,                  // FINEST
            500,                  // FINER, FINE
            800,                  // CONFIG, INFO
            900,                  // WARNING
            1000,                 // SEVERE
    };

    private static ZorkaLogLevel[] jdkLevels = {
            ZorkaLogLevel.TRACE,  // FINEST
            ZorkaLogLevel.DEBUG,  // FINER, FINE
            ZorkaLogLevel.INFO,   // CONFIG, INFO
            ZorkaLogLevel.WARN,   // WARNING
            ZorkaLogLevel.ERROR,  // SEVERE
            ZorkaLogLevel.FATAL   // everything above
    };


    public LogRecord toLogRecord(Object orig) {
        if (orig != null) {
            if ("java.util.logging.LogRecord".equals(orig.getClass().getName())) {
                return fromJdk(orig);
            }
        }

        return null;
    }


    private LogRecord fromJdk(Object orig) {

        // TODO we propably don't need to use ObjectInspector as java.util.logging is a part of JDK

        Integer level = inspector.get(orig, "level", "intValue()");

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

        return new LogRecord(logLevel,
                (String)inspector.get(orig, "sourceClassName"),
                (String)inspector.get(orig, "sourceMethodName"),
                (String)inspector.get(orig, "message"),
                (Throwable)inspector.get(orig, "thrown"));
    }
}
