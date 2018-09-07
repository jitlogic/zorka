package com.jitlogic.zorka.core.test.spy.support.cbor;

import java.util.ArrayList;
import java.util.List;

public class STErr {

    private int classId;
    private String className;
    private String message;
    private List<StackTraceElement> stack = new ArrayList<StackTraceElement>();

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
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

    public List<StackTraceElement> getStack() {
        return stack;
    }

    public void setStack(List<StackTraceElement> stack) {
        this.stack = stack;
    }
}
