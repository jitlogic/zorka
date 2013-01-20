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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.common;

import java.util.List;
import java.util.ArrayList;

/**
 * This has been written from scratch in order to not interfere with
 * other logging frameworks.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLogger implements ZorkaTrapper {


    /** Logger */
    private static ZorkaLogger logger = new ZorkaLogger();


    /**
     *  Returns client-side logger object
     *
     * @param clazz source class
     *
     * @return ZorkaLog object
     */
    public static ZorkaLog getLog(Class<?> clazz) {
        String[] segs = clazz.getName().split("\\.");
        return getLog(segs[segs.length-1]);
    }


    /**
     *  Returns client-side logger object
     *
     * @param tag log tag
     *
     * @return ZorkaLog object
     */
    public static ZorkaLog getLog(String tag) {
        return new ZorkaLog(tag, logger);
    }


    /**
     * Returns logger instance
     *
     * @return logger
     */
    public static ZorkaLogger getLogger() {
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


    /** List of trappers that will receive log messages */
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
    public void addTrapper(ZorkaTrapper trapper) {
        trappers.add(trapper);
    }

    /**
     * Logs a message. Log message is sent to all registered trappers.
     *
     * @param logLevel log level
     *
     * @param tag log message tag (eg. component name)
     *
     * @param message message text (optionally format string)
     *
     * @param e exception thrown (if any)
     *
     * @param args optional argument used when message text is a format string
     */
    public void trap(ZorkaLogLevel logLevel, String tag, String message, Throwable e, Object... args) {
        for (ZorkaTrapper trapper : trappers) {
            trapper.trap(logLevel, tag, message, e, args);
        }
    }

}
