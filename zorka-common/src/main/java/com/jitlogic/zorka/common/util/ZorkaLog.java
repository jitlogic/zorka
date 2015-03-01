/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * 
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.common.util;

/**
 * ZorkaLog objects are used internally as loggers. Zorka agent avoids using
 * standard logger implementation in order to not interfere with monitored
 * applications.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLog {

    /** Custom tag (eg. component name) */
    private String tag;

    /** Output logger */
    private ZorkaLogger output;


    /**
     * Creates logger object.
     *
     * @param tag custom tag
     *
     * @param output output logger
     */
    public ZorkaLog(String tag, ZorkaLogger output) {
        this.tag = tag;
        this.output = output;
    }


    /**
     * Logs message with TRACE level
     *
     * @param msg message text
     *
     * @param args optional arguments
     */
	public void trace(long level, String msg, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
		    output.trap(ZorkaLogLevel.TRACE, tag, msg, null, args);
        }
	}


    /**
     * Logs message with TRACE level
     *
     * @param msg message text
     *
     * @param e exception thrown
     *
     * @param args optional arguments
     */
    public void trace(long level, String msg, Throwable e, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.TRACE, tag, msg, e, args);
        }
    }


    /**
     * Logs message with DEBUG level
     *
     * @param msg message text
     * @param args optional arguments
     */
	public void debug(long level, String msg, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap( ZorkaLogLevel.DEBUG, tag, msg, null, args);
        }
	}


    /**
     * Logs message with DEBUG level
     *
     * @param msg message text
     *
     * @param e exception object
     *
     * @param args optional arguments
     */
    public void debug(long level, String msg, Throwable e, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.DEBUG, tag, msg, e, args);
        }
    }


    /**
     * Logs message with INFO level
     *
     * @param msg message text
     *
     * @param args optional arguments
     */
	public void info(long level, String msg, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.INFO, tag, msg, null, args);
        }
	}


    /**
     * Logs message with WARNING level
     *
     * @param msg message text
     *
     * @param args optional arguments
     */
	public void warn(long level, String msg, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.WARN, tag, msg, null, args);
        }
	}


    /**
     * Logs message with ERROR level
     *
     * @param msg message text
     *
     * @param args optional arguments
     */
	public void error(long level, String msg, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.ERROR, tag, msg, null, args);
        }
	}


    /**
     * Logs message with ERROR level
     *
     * @param msg message text
     *
     * @param e exception to log
     *
     * @param args optional arguments
     */
    public void error(long level, String msg, Throwable e, Object...args) {
        if (ZorkaLogger.isLogMask(level)) {
            output.trap(ZorkaLogLevel.ERROR, tag, msg, e, args);
        }
	}


}
