package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;

import static org.slf4j.spi.LocationAwareLogger.*;

public class ZorkaTrapperLogger extends MarkerIgnoringBase {

    private volatile int logLevel;
    private volatile ZorkaTrapper trapper;
    private String shortName;

    public ZorkaTrapperLogger(String name, int logLevel, ZorkaTrapper trapper) {
        this.name = name;
        this.logLevel = logLevel;
        this.trapper = trapper;
        this.shortName = name.substring(name.lastIndexOf('.')+1);
    }

    public int getLogLevel() {
        return logLevel;
    }

    public synchronized void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public synchronized void setTrapper(ZorkaTrapper trapper) {
        this.trapper = trapper;
    }

    @Override
    public boolean isTraceEnabled() {
        return logLevel <= TRACE_INT;
    }

    @Override
    public void trace(String msg) {
        if (logLevel <= TRACE_INT) {
            trapper.trap(ZorkaLogLevel.TRACE, shortName, msg, null);
        }
    }

    @Override
    public void trace(String msg, Object arg1) {
        if (logLevel <= TRACE_INT) {
            trapper.trap(ZorkaLogLevel.TRACE, shortName, msg, null, arg1);
        }
    }

    @Override
    public void trace(String msg, Object arg1, Object arg2) {
        if (logLevel <= TRACE_INT) {
            trapper.trap(ZorkaLogLevel.TRACE, shortName, msg, null, arg1, arg2);
        }
    }

    @Override
    public void trace(String msg, Object...args) {
        if (logLevel <= TRACE_INT) {
            trapper.trap(ZorkaLogLevel.TRACE, shortName, msg, null, args);
        }
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (logLevel <= TRACE_INT) {
            trapper.trap(ZorkaLogLevel.TRACE, shortName, msg, e);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return logLevel <= DEBUG_INT;
    }

    @Override
    public void debug(String msg) {
        if (logLevel <= DEBUG_INT) {
            trapper.trap(ZorkaLogLevel.DEBUG, shortName, msg, null);
        }
    }

    @Override
    public void debug(String msg, Object arg1) {
        if (logLevel <= DEBUG_INT) {
            trapper.trap(ZorkaLogLevel.DEBUG, shortName, msg, null, arg1);
        }
    }

    @Override
    public void debug(String msg, Object arg1, Object arg2) {
        if (logLevel <= DEBUG_INT) {
            trapper.trap(ZorkaLogLevel.DEBUG, shortName, msg, null, arg1, arg2);
        }
    }

    @Override
    public void debug(String msg, Object...args) {
        if (logLevel <= DEBUG_INT) {
            trapper.trap(ZorkaLogLevel.DEBUG, shortName, msg, null, args);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (logLevel <= DEBUG_INT) {
            trapper.trap(ZorkaLogLevel.DEBUG, shortName, msg, e);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logLevel <= INFO_INT;
    }

    @Override
    public void info(String msg) {
        if (logLevel <= INFO_INT) {
            trapper.trap(ZorkaLogLevel.INFO, shortName, msg, null);
        }
    }

    @Override
    public void info(String msg, Object arg1) {
        if (logLevel <= INFO_INT) {
            trapper.trap(ZorkaLogLevel.INFO, shortName, msg, null, arg1);
        }
    }

    @Override
    public void info(String msg, Object arg1, Object arg2) {
        if (logLevel <= INFO_INT) {
            trapper.trap(ZorkaLogLevel.INFO, shortName, msg, null, arg1, arg2);
        }
    }

    @Override
    public void info(String msg, Object...args) {
        if (logLevel <= INFO_INT) {
            trapper.trap(ZorkaLogLevel.INFO, shortName, msg, null, args);
        }
    }

    @Override
    public void info(String msg, Throwable e) {
        if (logLevel <= INFO_INT) {
            trapper.trap(ZorkaLogLevel.INFO, shortName, msg, e);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logLevel <= WARN_INT;
    }

    @Override
    public void warn(String msg) {
        if (logLevel <= WARN_INT) {
            trapper.trap(ZorkaLogLevel.WARN, shortName, msg, null);
        }
    }

    @Override
    public void warn(String msg, Object arg1) {
        if (logLevel <= WARN_INT) {
            trapper.trap(ZorkaLogLevel.WARN, shortName, msg, null, arg1);
        }
    }

    @Override
    public void warn(String msg, Object...args) {
        if (logLevel <= WARN_INT) {
            trapper.trap(ZorkaLogLevel.WARN, shortName, msg, null, args);
        }
    }

    @Override
    public void warn(String msg, Object arg1, Object arg2) {
        if (logLevel <= WARN_INT) {
            trapper.trap(ZorkaLogLevel.WARN, shortName, msg, null, arg1, arg2);
        }
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (logLevel <= WARN_INT) {
            trapper.trap(ZorkaLogLevel.WARN, shortName, msg, e);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logLevel <= ERROR_INT;
    }

    @Override
    public void error(String msg) {
        if (logLevel <= ERROR_INT) {
            trapper.trap(ZorkaLogLevel.ERROR, shortName, msg, null);
        }
    }

    @Override
    public void error(String msg, Object arg1) {
        if (logLevel <= ERROR_INT) {
            trapper.trap(ZorkaLogLevel.ERROR, shortName, msg, null, arg1);
        }
    }

    @Override
    public void error(String msg, Object arg1, Object arg2) {
        if (logLevel <= ERROR_INT) {
            trapper.trap(ZorkaLogLevel.ERROR, shortName, msg, null, arg1, arg2);
        }
    }

    @Override
    public void error(String msg, Object...args) {
        if (logLevel <= ERROR_INT) {
            trapper.trap(ZorkaLogLevel.ERROR, shortName, msg, null, args);
        }
    }

    @Override
    public void error(String msg, Throwable e) {
        if (logLevel <= ERROR_INT) {
            trapper.trap(ZorkaLogLevel.ERROR, shortName, msg, e);
        }
    }
}
