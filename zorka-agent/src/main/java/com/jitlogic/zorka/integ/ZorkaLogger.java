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
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZorkaLogger {

    private static ZorkaLogger logger = null;


    public static ZorkaLog getLog(Class<?> clazz) {
        String[] segs = clazz.getName().split("\\.");
        return getLog(segs[segs.length-1]);
    }


    public synchronized  static ZorkaLog getLog(String tag) {
        if (logger == null) {
            logger = new ZorkaLogger();
        }

        return new ZorkaLog(tag, logger);
    }


    public synchronized static ZorkaLogger getLogger() {
        return logger;
    }


    public synchronized static void setLogger(ZorkaLogger newLogger) {
        logger = newLogger;
    }


    private List<ZorkaTrapper> trappers = new ArrayList<ZorkaTrapper>();

    public ZorkaLogger() {

    }

    public void init(Properties props) {
        initFileTrapper(props);

        if ("yes".equalsIgnoreCase(props.getProperty("zorka.syslog", "no").trim())) {
            initSyslogTrapper(props);
        }
    }

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


    public void log(ZorkaLogLevel logLevel, String tag, String message, Throwable e, Object... args) {
        for (ZorkaTrapper trapper : trappers) {
            trapper.trap(logLevel, tag, message, e, args);
        }
    }

}
