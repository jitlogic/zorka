/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.util;

//import com.jitlogic.zorka.core.AgentDiagnostics;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * File trapper implements trapper interface that logs messages to local file.
 * Two kinds of log files are defined: rolling (rotating) log and daily log
 * (with yyyy-mm-dd suffixes).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class FileTrapper extends ZorkaAsyncThread<String> implements ZorkaTrapper {

    public static volatile boolean ENABLE_FSYNC = false;

    /**
     * Rolling (rotating) file trapper maintains limited numebr of archived logs.
     */
    private static final int ROLLING = 1;

    /**
     * Daily file trapper mainatains log files by date (yyyy-mm-dd)
     */
    private static final int DAILY = 2;

    /**
     * Base log file
     */
    private final File logFile;

    /**
     * Trapper type
     */
    private final int type;

    /**
     * Maximum number of logs (only for rolling trappers)
     */
    private final int maxLogs;

    /**
     * Maximum log size (only for rolling trappers)
     */
    private final long maxSize;

    /**
     * Logs stack traces of exceptions if set to true
     */
    private boolean logExceptions = true;

    /**
     * Output (as print stream)
     */
    private PrintStream out;

    /**
     * Output (as output stream)
     */
    private FileOutputStream os;

    /**
     * Current log size (for rolling trappers)
     */
    private long currentSize;

    /**
     * Current suffix (for daily trappers)
     */
    private String currentSuffix;

    /**
     * Creates new rolling trapper.
     *
     * @param logLevel      log level
     * @param logPath       path to log file
     * @param maxLogs       maximum number of archived logs
     * @param maxSize       maximum log file size
     * @param logExceptions logs stack traces of exceptions if true
     * @return new file trapper
     */
    public static FileTrapper rolling(ZorkaLogLevel logLevel, String logPath, int maxLogs, long maxSize, boolean logExceptions) {
        return new FileTrapper(new File(logPath), ROLLING, maxLogs, maxSize, logExceptions);
    }


    /**
     * Creates new daily trapper
     *
     * @param logLevel      log level
     * @param logPath       path to log file
     * @param logExceptions log stack traces of exceptions if true
     * @return new file trapper
     */
    public static FileTrapper daily(ZorkaLogLevel logLevel, String logPath, boolean logExceptions) {
        return new FileTrapper(new File(logPath), DAILY, 0, Long.MAX_VALUE, logExceptions);
    }


    /**
     * Standard constructor (not publicly available - use static methods instead).
     *
     * @param logFile       log file path (as File object)
     * @param type          trapper type
     * @param maxLogs       max number of logs (irrelevant for daily trappers)
     * @param size          log size (irrelevant for daily trappers)
     * @param logExceptions log stack traces of exceptions if true
     */
    private FileTrapper(File logFile, int type, int maxLogs, long size, boolean logExceptions) {
        super(logFile.getName());
        this.logFile = logFile;
        this.type = type;
        this.maxLogs = maxLogs;
        this.maxSize = size;
        this.logExceptions = logExceptions;
    }


    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String message, Throwable e, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        sb.append(" ");
        sb.append(logLevel);
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

        // TODO AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SUBMITTED);

        if (!submit(sb.toString())) {
            // TODO AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_DROPPED);
        }

    }


    /**
     * Formats string. Used by other trapper functions. If there are no arguments, string formatting
     * is skipped and message string (template) is returned right away. See String.format() description
     * for more information about string formatting.
     *
     * @param message message string (template)
     * @param args    arguments for String.format() function
     * @return formatted string
     */
    public String format(String message, Object... args) {
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
    protected void process(List<String> msgs) {

        switch (type) {
            case ROLLING:
                if (currentSize > maxSize) {
                    reset();
                }
                break;
            case DAILY:
                if (!new SimpleDateFormat("yyyy-MM-dd").format(new Date()).equals(currentSuffix)) {
                    reset();
                }
                break;
        }

        if (out != null) {

            for (String s : msgs) {
                out.println(s);
                currentSize += s.getBytes().length + 1;
                AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SENT);
            }

            out.flush();

            fsync();
        }
    }

    private void fsync() {
        if (ENABLE_FSYNC) {
            try {
                os.flush();
                os.getFD().sync();
            } catch (IOException e) {
            }
        }
    }


    @Override
    public void open() {
        reset();
    }


    @Override
    public void close() {
        if (out != null) {
            out.close();
            out = null;
        }

        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                if (log != null) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error closing log file " + logFile, e);
                }
            }
            os = null;
        }
    }


    /**
     * Resets trapper. That means closing log file and opening it again (or a new one).
     * Depending on trapper type, rotation or reopening (possibly with new date suffix) is performed.
     */
    private void reset() {

        close();

        switch (type) {
            case ROLLING:
                roll();
                break;
            case DAILY:
                reopen();
                break;
        }
    }


    /**
     * Performs log file rotation for rolling trappers.
     */
    private void roll() {
        File f = new File(logFile.getPath() + "." + maxLogs);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxLogs - 1; i >= 0; i--) {
            f = new File(logFile.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(logFile.getPath() + "." + (i + 1));
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
                log.error(ZorkaLogger.ZAG_ERRORS, "Error opening log file " + logFile, e);
            }
        }
    }


    /**
     * Performs file reopen for daily trappers.
     */
    private void reopen() {
        currentSuffix = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File f = new File(logFile.getAbsolutePath() + "." + currentSuffix);
        try {
            os = new FileOutputStream(f, true);
            out = new PrintStream(os);
            currentSize = f.exists() ? f.length() : 0;
        } catch (Exception e) {
            if (log != null) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Cannot open log file " + f.getAbsolutePath(), e);
            }
        }
    }
}
