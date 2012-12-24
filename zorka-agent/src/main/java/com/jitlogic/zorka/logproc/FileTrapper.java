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

import com.jitlogic.zorka.agent.ZorkaTrapper;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogLevel;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileTrapper extends ZorkaTrapper<String> implements LogProcessor {

    private final static int ROLLING = 1;
    private final static int DATED   = 2;

    private final static SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static SimpleDateFormat fformat = new SimpleDateFormat("yyyy-MM-dd");

    private final File logFile;
    private final int type;
    private final int maxLogs;
    private final long maxSize;
    private final boolean logExceptions;

    private final ZorkaLog log = null;

    private PrintStream out = null;
    private OutputStream os = null;
    private long currentSize = 0;
    private String currentSuffix = null;

    private ZorkaLogLevel logLevel;

    public static FileTrapper rolling(ZorkaLogLevel logLevel, String path, int count, long size, boolean logExceptions) {
        return new FileTrapper(logLevel, new File(path), ROLLING, count, size, logExceptions);
    }


    public static FileTrapper daily(ZorkaLogLevel logLevel, String path, boolean logExceptions) {
        return new FileTrapper(logLevel, new File(path), DATED, 0, Long.MAX_VALUE, logExceptions);
    }


    private FileTrapper(ZorkaLogLevel logLevel, File logFile, int type, int maxLogs, long size, boolean logExceptions) {
        super(logFile.getName());
        this.logLevel = logLevel;
        this.logFile = logFile;
        this.type = type;
        this.maxLogs = maxLogs;
        this.maxSize = size;
        this.logExceptions = logExceptions;
    }


    public void logTrace(String tag, String message, Object...args) {
        logTrace(tag, message,  null, args);
    }

    public void logTrace(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.TRACE, message,  e, args);
    }

    public void logDebug(String tag, String message, Object...args) {
        logDebug(tag, message,  null, args);
    }

    public void logDebug(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.DEBUG, message,  e, args);
    }

    public void logInfo(String tag, String message, Object...args) {
        logInfo(tag, message,  null, args);
    }

    public void logInfo(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.INFO, message,  e, args);
    }

    public void logWarn(String tag, String message, Object...args) {
        logWarn(tag, message,  null, args);
    }

    public void logWarn(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.WARN, message,  e, args);
    }

    public void logError(String tag, String message, Object...args) {
        logError(tag, message,  null, args);
    }

    public void logError(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.ERROR, message,  e, args);
    }

    public void logFatal(String tag, String message, Object...args) {
        logFatal(tag, message,  null, args);
    }

    public void logFatal(String tag, String message, Throwable e, Object...args) {
        log(tag, ZorkaLogLevel.FATAL, message,  e, args);
    }

    public void log(String tag, ZorkaLogLevel logLevel, String message, Throwable e, Object...args) {
        StringBuilder sb = new StringBuilder();
        sb.append(dformat.format(new Date()));
        sb.append(" ");
        sb.append(tag);
        sb.append(" ");
        sb.append(format(message, args));

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

        submit(sb.toString());
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


    @Override
    protected void process(String msg) {

        switch (type) {
            case ROLLING:
                if (currentSize > maxSize) {
                    reset();
                }
                break;
            case DATED:
                if (!fformat.format(new Date()).equals(currentSuffix)) {
                    reset();
                }
                break;
        }

        if (out != null) {
            out.println(msg);
            currentSize += msg.getBytes().length + 1;
        }
    }


    @Override
    protected void open() {
        reset();
    }


    @Override
    protected void close() {
        if (out != null) {
            out.close();
            out = null;
        }

        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                if (log != null) {
                    log.error("Error closing log file " + logFile, e);
                }
            }
            os = null;
        }
    }


    private void reset() {

        close();

        switch (type) {
            case ROLLING:
                roll(); break;
            case DATED:
                reopen(); break;
        }
    }


    private void roll() {
        String logDir = logFile.getParent(), logFileName = logFile.getName();

        File f = new File(logFile.getPath() + "." + maxLogs);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxLogs-1; i >= 0; i--) {
            f = new File(logFile.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(logFile.getPath() + "." + (i+1));
                f.renameTo(nf);
            }
        }


        File nf = new File(logFile.getPath() + ".0");
        logFile.renameTo(nf);

        try {
            os = new FileOutputStream(logFile);
            out = new PrintStream(os);
            currentSize = 0;
        } catch (Exception e) {
            if (log != null) {
                log.error("Error opening log file " + logFile, e);
            }
        }
    }


    private void reopen() {
        currentSuffix = fformat.format(new Date());
        File f = new File(logFile.getAbsolutePath() + "." + currentSuffix);
        try {
            os = new FileOutputStream(f, true);
            out = new PrintStream(os);
            currentSize = f.exists() ? f.length() : 0;
        } catch (Exception e) {
            if (log != null) {
                log.error("Cannot open log file " + f.getAbsolutePath(), e);
            }
        }
    }

    public LogRecord process(LogRecord rec) {
        if (logLevel == null || rec.getLogLevel().getPriority() >= logLevel.getPriority()) {
            log(rec.getOriginClass(), rec.getLogLevel(), rec.getMessage(), rec.getException());
        }

        return rec;
    }
}
