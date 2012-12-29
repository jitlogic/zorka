package com.jitlogic.zorka.logproc;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.integ.syslog.SyslogLib;
import com.jitlogic.zorka.integ.syslog.SyslogTrapper;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.*;
import java.util.Date;
import java.util.Properties;

import static com.jitlogic.zorka.agent.ZorkaConfig.*;

/**
 * This has been written from scratch in order to not interfere with
 * other logging frameworks.
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZorkaLogger {

    // TODO uporzadkowac logger do konca

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


    private String logDir, logFileName;
    private boolean logExceptions = false;
    private boolean doTrace;
    private ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

    private int maxSize = 10*1024*1024;
    private int maxLogs = 4;

    private boolean active = true;

    private PrintStream out = null;
    private OutputStream os = null;
    private int currentSize = 0;

    private SyslogTrapper syslog = null;
    private int syslogFacility = SyslogLib.F_LOCAL0;

    public ZorkaLogger() {
        logDir = ZorkaConfig.getLogDir();
        Properties props = ZorkaConfig.getProperties();
        logExceptions = "yes".equalsIgnoreCase(props.getProperty(ZORKA_LOG_EXCEPTIONS));
        doTrace = "yes".equalsIgnoreCase(props.getProperty(ZORKA_LOG_TRACE));
        logFileName = props.getProperty(ZORKA_LOG_FNAME).trim();

        try {
            logThreshold = ZorkaLogLevel.valueOf (props.getProperty(ZORKA_LOG_LEVEL));
            maxSize = ZorkaUtil.parseIntSize(props.getProperty(ZORKA_LOG_SIZE).trim());
            maxLogs = ZorkaUtil.parseIntSize(props.getProperty(ZORKA_LOG_NUM).trim());

            if ("yes".equalsIgnoreCase(props.getProperty(ZORKA_SYSLOG, "no").trim())) {
                String server = props.getProperty(ZORKA_SYSLOG_SERVER, "127.0.0.1").trim();
                String hostname = props.getProperty(ZORKA_HOSTNAME, "zorka").trim();
                syslog = new SyslogTrapper(server, hostname, true);
                syslogFacility = SyslogLib.getFacility(props.getProperty(ZORKA_SYSLOG_FACILITY, "F_LOCAL0").trim());
                syslog.start();
            }

        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public synchronized void log(String tag, ZorkaLogLevel logLevel, String message, Throwable e, Object...args) {
        if (active && logLevel.getPriority() >= logThreshold.getPriority()) {

            if (out == null || currentSize >= maxSize) {
                reopen();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(new Date());
            sb.append(" ");
            sb.append(tag);
            sb.append(" ");
            String fmsg = format(message, args);
            sb.append(fmsg);

            if (e != null) {
                sb.append(" [");
                sb.append(e.toString());
                sb.append("]");

                if (logExceptions) {
                    StringWriter sw = new StringWriter(512);
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    sb.append("\n");
                    sb.append(sw.toString());
                }
            }

            String s = sb.toString();

            if (out != null)
                out.println(s);
            currentSize += s.getBytes().length + 1;

            if (syslog != null) {
                syslog.log(logLevel.getSeverity(), syslogFacility, tag, fmsg);
            }

        }
    }


    public String format(String message, Object...args) {
        if (args.length == 0) {
            return message;
        } else {
            try {
                return String.format(message, args);
            } catch (Exception e) {
                return "Invalid format '" + message + "' [" + ZorkaUtil.join(",", args) + "]: " + e;
            }
        }
    }



    private void reopen() {
        if (out != null) {
            out.close();
            out = null;
        }

        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
            }
            os = null;
        }

        try {
            rotate();
            os = new FileOutputStream(logDir + "/" + logFileName);
            out = new PrintStream(os);
            currentSize = 0;
        } catch (Exception e) {
            System.err.println("Error reopening log: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void rotate() {
        // TODO get rid of logging code, use FileTrapper instead
        File f = new File(logDir + "/" + logFileName + "." + maxLogs);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxLogs-1; i >= 0; i--) {
            f = new File(logDir + "/" + logFileName + "." + i);
            if (f.exists()) {
                File nf = new File(logDir + "/" + logFileName + "." + (i+1));
                f.renameTo(nf);
            }
        }

        f = new File(logDir + "/" + logFileName);
        File nf = new File(logDir + "/" + logFileName + ".0");
        f.renameTo(nf);
    }
}
