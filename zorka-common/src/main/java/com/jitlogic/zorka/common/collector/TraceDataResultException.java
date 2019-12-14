package com.jitlogic.zorka.common.collector;

import java.util.ArrayList;
import java.util.List;

public class TraceDataResultException {

    private long id;

    /** Exception class name */
    private String className;

    /** Exception message */
    private String message;

    /** Stack trace */
    private List<String> stack = new ArrayList<String>();

    /** Cause (if any) */
    private TraceDataResultException cause;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getStack() {
        return stack;
    }

    public void setStack(List<String> stack) {
        this.stack = stack;
    }

    public TraceDataResultException getCause() {
        return cause;
    }

    public void setCause(TraceDataResultException cause) {
        this.cause = cause;
    }
}
