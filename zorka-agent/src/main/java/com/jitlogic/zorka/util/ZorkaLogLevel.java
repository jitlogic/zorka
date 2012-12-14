package com.jitlogic.zorka.util;

/**
 */
public enum ZorkaLogLevel {

    TRACE(0, 7),
    DEBUG(1, 7),
    INFO(2, 6),
    WARN(3, 4),
    ERROR(4, 3),
    FATAL(5, 0);

    private final int priority;
    private final int severity;

    private ZorkaLogLevel(int priority, int severity) {
        this.priority = priority;
        this.severity = severity;
    }

    public int getPriority() {
        return priority;
    }

    public int getSeverity() {
        return severity;
    }
}
