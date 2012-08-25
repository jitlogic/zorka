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
    private boolean logExceptions;
    private boolean doTrace;
    private ZorkaLogLevel logThreshold;

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
        logThreshold = ZorkaLogLevel.valueOf (ZorkaConfig.get("zorka.log.level", "DEBUG"));
    }

    public void log(ZorkaLog source, ZorkaLogLevel logLevel, String message) {
        log(source, logLevel, message, null);
    }

    public void log(ZorkaLog source, ZorkaLogLevel logLevel, String message, Throwable e) {
        if (active && logLevel.getPriority() >= logThreshold.getPriority()) {
            if (out == null || currentSize >= maxSize) {
                reopen();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(new Date());
            sb.append(" ");
            sb.append(message);

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
            // TODO tutaj przerolowanie pliku
            os = new FileOutputStream(logDir + "/zorka.log");
            out = new PrintStream(os);
        } catch (FileNotFoundException e) {
        }
    }
}
