/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.viewer;

import org.objectweb.asm.Type;

import java.text.SimpleDateFormat;
import java.util.*;

public class NamedTraceRecord {

    /** Overflow record will be discarded regardless of method execution time and other conditions. */
    public static final int OVERFLOW_FLAG = 0x0001;

    /** Indicates that new trace has been started from here. */
    public static final int TRACE_BEGIN   = 0x0002;

    /** Exception thrown from method called from frame hasn't been handled and has been thrown out of current frame. */
    public static final int EXCEPTION_PASS = 0x0004;

    /** Exception thrown from method called current frame has been wrapped and thrown out of current frame. */
    public static final int EXCEPTION_WRAP = 0x0008;

    private String traceName, className, methodName, methodSignature;
    private long clock;

    private long time;
    private long errors, calls;
    private int flags, level, records;
    private int traceFlags;

    private NamedTraceRecord parent;
    private List<NamedTraceRecord> children;
    private Map<String,Object> attrs;

    private Object exception;

    private double timePct;

    public NamedTraceRecord(NamedTraceRecord parent) {
        this.parent = parent;
    }


    public NamedTraceRecord getParent() {
        return parent;
    }


    public String getTraceName() {
        return traceName;
    }


    public void setTraceName(String traceName) {
        this.traceName = traceName;
    }


    public String getClassName() {
        return className;
    }


    public void setClassName(String className) {
        this.className = className;
    }


    public String getMethodName() {
        return methodName;
    }


    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    public String getMethodSignature() {
        return methodSignature;
    }


    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }


    public long getClock() {
        return clock;
    }


    public void setClock(long clock) {
        this.clock = clock;
    }


    public long getTime() {
        return time;
    }


    public void setTime(long time) {
        this.time = time;
    }


    public long getErrors() {
        return errors;
    }


    public void setErrors(long errors) {
        this.errors = errors;
    }


    public long getCalls() {
        return calls;
    }


    public void setCalls(long calls) {
        this.calls = calls;
    }


    public int getFlags() {
        return flags;
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getLevel() {
        return level;
    }

    public int getRecords() {
        return records;
    }

    public Object getException() {
        return exception;
    }


    public void setException(Object exception) {
        this.exception = exception;
    }


    public int getTraceFlags() {
        return traceFlags;
    }

    public void setTraceFlags(int traceFlags) {
        this.traceFlags = traceFlags;
    }

    public double getTimePct() {
        return timePct;
    }


    public Object getAttr(String attrName) {
        return attrs != null ? attrs.get(attrName) : null;
    }


    public void setAttr(String attrName, Object attrVal) {
        if (attrs == null) {
            attrs = new HashMap<String,Object>();
        }
        attrs.put(attrName, attrVal);
    }


    public Map<String,Object> getAttrs() {
        return Collections.unmodifiableMap(attrs != null ? attrs : new HashMap<String, Object>());
    }

    public int numAttrs() {
        return attrs != null ? attrs.size() : 0;
    }


    public void addChild(NamedTraceRecord child) {
        if (children == null) {
            children = new ArrayList<NamedTraceRecord>();
        }
        children.add(child);
    }


    public NamedTraceRecord getChild(int n) {
        return (children != null && n < children.size()) ? children.get(n) : null;
    }


    public int numChildren() {
        return children != null ? children.size() : 0;
    }


    public List<NamedTraceRecord> getChildren() {
        return Collections.unmodifiableList(children != null ? children : new ArrayList<NamedTraceRecord>(1));
    }

    public static final int PS_SHORT_CLASS = 0x01;
    public static final int PS_RESULT_TYPE = 0x02;
    public static final int PS_SHORT_ARGS  = 0x04;
    public static final int PS_NO_ARGS     = 0x08;


    public String prettyPrint() {
        return prettyPrint(PS_RESULT_TYPE|PS_SHORT_ARGS);
    }


    public String prettyPrint(int style) {
        StringBuffer sb = new StringBuffer(128);

        // Print return type
        if (0 != (style & PS_RESULT_TYPE)) {
            Type retType = Type.getReturnType(getMethodSignature());
            if (0 != (style & PS_SHORT_ARGS)) {
                sb.append(ViewerUtil.shortClassName(retType.getClassName()));
            } else {
                sb.append(retType.getClassName());
            }
            sb.append(" ");
        }

        // Print class name
        if (0 != (style & PS_SHORT_CLASS)) {
            sb.append(ViewerUtil.shortClassName(getClassName()));
        } else {
            sb.append(getClassName());
        }

        sb.append(".");
        sb.append(getMethodName());
        sb.append("(");

        // Print arguments (if needed)
        if (0 == (style & PS_NO_ARGS)) {
            Type[] types = Type.getArgumentTypes(getMethodSignature());
            for (int i = 0; i < types.length; i++) {
                if (i > 0) { sb.append(", "); }
                if (0 != (style & PS_SHORT_ARGS)) {
                    sb.append(ViewerUtil.shortClassName(types[i].getClassName()));
                } else {
                    sb.append(types[i].getClassName());
                }
            }
        }

        sb.append(")");

        return sb.toString();
    }


    public String prettyClock() {
        return new SimpleDateFormat("hh:mm:ss.SSS").format(getClock());
    }


    public void fixup(long total, int level) {
        timePct = 100.0 * this.time / total;
        this.level = level;
        this.records = 1;

        if (children != null) {
            for (NamedTraceRecord child : children) {
                child.fixup(total, level+1);
                records += child.records;
            }
        }
    }


    public void scanAttrs(List<String[]> result) {
        if (attrs != null) {
            for (Map.Entry e : attrs.entrySet()) {
                result.add(new String[] { e.getKey().toString(), ""+e.getValue()});
            }
        }

        if (children != null) {
            for (NamedTraceRecord rec : children) {
                rec.scanAttrs(result);
            }
        }
    }

    public void scanRecords(List<NamedTraceRecord> result) {
        result.add(this);

        if (children != null) {
            for (NamedTraceRecord child : children) {
                child.scanRecords(result);
            }
        }
    }
}
