package com.jitlogic.zorka.util;

import java.io.*;
import java.util.Date;

/**
 * This has been written from scratch in order to not interfere with
 * other logging frameworks.
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZorkaLogger {

    private static ZorkaLogger logger = null;

    public synchronized static ZorkaLog getLog(Class<?> clazz) {

        if (logger == null) {
            logger = new ZorkaLogger();
        }

        return new ZorkaLog(clazz, logger);
    }

    private String logDir;
    private boolean logExceptions = false;
    private boolean doTrace;
    private ZorkaLogLevel logThreshold = ZorkaLogLevel.DEBUG;

    private int maxSize = 10*1024*1024;
    private int maxLogs = 4;

    private boolean active = true;

    private PrintStream out = null;
    private OutputStream os = null;
    private int currentSize = 0;


    public ZorkaLogger() {
        logDir = ZorkaConfig.getLogDir();
        logExceptions = "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.log.exceptions", "yes"));
        doTrace = "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.log.trace", "no"));

        try {
            logThreshold = ZorkaLogLevel.valueOf (ZorkaConfig.get("zorka.log.level", "DEBUG"));
            maxSize = ZorkaUtil.parseIntSize(ZorkaConfig.get("zorka.log.size", "1048576").trim());
            maxLogs = ZorkaUtil.parseIntSize(ZorkaConfig.get("zorka.log.fnum", "4").trim());
            logExceptions = "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.log.exceptions", "no").trim());
        } catch (Exception e) {
            System.err.println("Error parsing logger arguments: " + e.getMessage());
            e.printStackTrace();
        }

    }


    public void log(ZorkaLog source, ZorkaLogLevel logLevel, String message) {
        log(source, logLevel, message, null);
    }


    public synchronized void log(ZorkaLog source, ZorkaLogLevel logLevel, String message, Throwable e) {
        if (active && logLevel.getPriority() >= logThreshold.getPriority()) {
            if (out == null || currentSize >= maxSize) {
                reopen();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(new Date());
            sb.append(" ");
            sb.append(message);

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
            os = new FileOutputStream(logDir + "/zorka.log");
            out = new PrintStream(os);
            currentSize = 0;
        } catch (Exception e) {
            System.err.println("Error reopening log: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void rotate() {
        File f = new File(logDir + "/zorka.log." + maxLogs);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxLogs-1; i >= 0; i--) {
            f = new File(logDir + "/zorka.log." + i);
            if (f.exists()) {
                File nf = new File(logDir + "/zorka.log." + (i+1));
                f.renameTo(nf);
            }
        }

        f = new File(logDir + "/zorka.log");
        File nf = new File(logDir + "/zorka.log.0");
        f.renameTo(nf);
    }
}
