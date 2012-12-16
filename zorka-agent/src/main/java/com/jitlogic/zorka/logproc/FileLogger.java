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

import com.jitlogic.zorka.agent.FileTrapper;
import com.jitlogic.zorka.util.ZorkaLogLevel;

public class FileLogger implements LogProcessor {


    public static FileLogger daily(ZorkaLogLevel logLevel, String path, boolean logExceptions) {

        FileTrapper trapper = FileTrapper.daily(path, logExceptions);
        trapper.start();

        return new FileLogger(logLevel, trapper);
    }


    public static FileLogger rolling(ZorkaLogLevel logLevel, String path, int count, long size, boolean logExceptions) {

        FileTrapper trapper = FileTrapper.rolling(path, count, size, logExceptions);
        trapper.start();

        return new FileLogger(logLevel, trapper);
    }

    private ZorkaLogLevel logLevel;
    private FileTrapper trapper;

    private FileLogger(ZorkaLogLevel logLevel, FileTrapper trapper) {
        this.logLevel = logLevel;
        this.trapper = trapper;
    }

    public LogRecord process(LogRecord rec) {

        if (rec.getLogLevel().getPriority() >= logLevel.getPriority()) {
            trapper.log(rec.getOriginClass(), rec.getLogLevel(), rec.getMessage(), rec.getException());
        }

        return rec;
    }
}
