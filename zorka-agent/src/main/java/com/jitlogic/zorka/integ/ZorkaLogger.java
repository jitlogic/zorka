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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.integ;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This has been written from scratch in order to not interfere with
 * other logging frameworks.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaLogger {


    /** Logger */
    private static ZorkaLogger logger = null;


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
    public synchronized  static ZorkaLog getLog(String tag) {
        if (logger == null) {
            logger = new ZorkaLogger();
        }

        return new ZorkaLog(tag, logger);
    }


    /**
     * Returns logger instance
     *
     * @return logger
     */
    public synchronized static ZorkaLogger getLogger() {
        return logger;
    }


    /**
     * Sets logger instance.
     *
     * @param newLogger new logger
     */
    public synchronized static void setLogger(ZorkaLogger newLogger) {
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
     * Adds and configures standard loggers.
     *
     * @param props configuration properties
     */
    public void init(Properties props) {
        initFileTrapper(props);

        if ("yes".equalsIgnoreCase(props.getProperty("zorka.syslog", "no").trim())) {
            initSyslogTrapper(props);
        }
    }

    /**
     * Creates and configures syslog trapper according to configuration properties
     *
     * @param props configuration properties
     */
    private void initSyslogTrapper(Properties props) {
        try {
            String server = props.getProperty("zorka.syslog.server", "127.0.0.1").trim();
            String hostname = props.getProperty("zorka.hostname", "zorka").trim();
            int syslogFacility = SyslogLib.getFacility(props.getProperty("zorka.syslog.facility", "F_LOCAL0").trim());

            SyslogTrapper syslog = new SyslogTrapper(server, hostname, syslogFacility, true);
            syslog.start();

            trappers.add(syslog);
        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Syslog trapper will be disabled.");
        }
    }


    /**
     * Creates and configures file trapper according to configuration properties
     *
     * @param props configuration properties
     */
    private void initFileTrapper(Properties props) {
        String logDir = ZorkaConfig.getLogDir();
        boolean logExceptions = "yes".equalsIgnoreCase(props.getProperty("zorka.log.exceptions"));
        String logFileName = props.getProperty("zorka.log.fname").trim();
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4*1024*1024, maxLogs = 4;

        try {
            logThreshold = ZorkaLogLevel.valueOf (props.getProperty("zorka.log.level"));
            maxSize = (int)ZorkaUtil.parseIntSize(props.getProperty("zorka.log.size").trim());
            maxLogs = (int)ZorkaUtil.parseIntSize(props.getProperty("zorka.log.num").trim());
        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
        }


        FileTrapper trapper = FileTrapper.rolling(logThreshold,
                new File(logDir, logFileName).getPath(), maxLogs, maxSize, logExceptions);
        trapper.start();

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
    public void log(ZorkaLogLevel logLevel, String tag, String message, Throwable e, Object... args) {
        for (ZorkaTrapper trapper : trappers) {
            trapper.trap(logLevel, tag, message, e, args);
        }
    }

}
