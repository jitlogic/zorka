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

import com.jitlogic.zorka.util.ZorkaLogLevel;

import java.util.HashMap;
import java.util.Map;

public class LogProcLib {

    private Map<String, FileTrapper> fileLoggers = new HashMap<String, FileTrapper>();

    public LogProcessor dailyLog(ZorkaLogLevel logLevel, String path, boolean logExceptions) {
        synchronized (fileLoggers) {
            if (!fileLoggers.containsKey(path)) {
                fileLoggers.put(path, FileTrapper.daily(logLevel, path, logExceptions));
            }
            return fileLoggers.get(path);
        }
    }

    public LogProcessor rollingLog(ZorkaLogLevel logLevel, String path, int count, long size, boolean logExceptions) {
        synchronized (fileLoggers) {
            if (!fileLoggers.containsKey(path)) {
                fileLoggers.put(path, FileTrapper.rolling(logLevel, path,  count,  size,  logExceptions));
            }
            return fileLoggers.get(path);
        }
    }

    public LogProcessor filter(ZorkaLogLevel logLevel, LogProcessor...filters) {
        return new CompositeLogProcessor(true, filters);
    }
}
