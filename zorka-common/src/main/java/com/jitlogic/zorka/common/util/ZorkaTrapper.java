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
package com.jitlogic.zorka.common.util;

/**
 * Zorka trapper interface. Objects implementing this interface can accept traps (logs)
 * and process them in arbitrary way.
 */
public interface ZorkaTrapper {

    /**
     * Sends log message.
     *
     * @param logLevel log level
     * @param tag      log tag
     * @param msg      message text (or format string)
     * @param e        exception object (or null if no exception has to be logged)
     * @param args     optional arguments (if log message is a format string)
     */
    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args);

}
