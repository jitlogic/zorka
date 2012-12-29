package com.jitlogic.zorka.logproc;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.integ.syslog.SyslogLib;
import com.jitlogic.zorka.integ.syslog.SyslogTrapper;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.jitlogic.zorka.agent.ZorkaConfig.*;

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
        Properties props = ZorkaConfig.getProperties();

        initFileTrapper(props);

        if ("yes".equalsIgnoreCase(props.getProperty(ZORKA_SYSLOG, "no").trim())) {
            initSyslogTrapper(props);
        }


    }


    private void initSyslogTrapper(Properties props) {
        try {
            String server = props.getProperty(ZORKA_SYSLOG_SERVER, "127.0.0.1").trim();
            String hostname = props.getProperty(ZORKA_HOSTNAME, "zorka").trim();
            int syslogFacility = SyslogLib.getFacility(props.getProperty(ZORKA_SYSLOG_FACILITY, "F_LOCAL0").trim());

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
        boolean logExceptions = "yes".equalsIgnoreCase(props.getProperty(ZORKA_LOG_EXCEPTIONS));
        String logFileName = props.getProperty(ZORKA_LOG_FNAME).trim();
        ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

        int maxSize = 4*1024*1024, maxLogs = 4;

        try {
            logThreshold = ZorkaLogLevel.valueOf (props.getProperty(ZORKA_LOG_LEVEL));
            maxSize = ZorkaUtil.parseIntSize(props.getProperty(ZORKA_LOG_SIZE).trim());
            maxLogs = ZorkaUtil.parseIntSize(props.getProperty(ZORKA_LOG_NUM).trim());
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
