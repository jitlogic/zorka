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

import com.jitlogic.zorka.core.util.SymbolicException;
import org.objectweb.asm.Type;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Trace record representation used by viewer application.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class NamedTraceRecord {

    /** Overflow record will be discarded regardless of method execution time and other conditions. */
    public static final int OVERFLOW_FLAG = 0x0001;

    /** Indicates that new trace has been started from here. */
    public static final int TRACE_BEGIN   = 0x0002;

    /** Exception thrown from method called from frame hasn't been handled and has been thrown out of current frame. */
    public static final int EXCEPTION_PASS = 0x0004;

    /** Exception thrown from method called current frame has been wrapped and thrown out of current frame. */
    public static final int EXCEPTION_WRAP = 0x0008;

    /** Indicates that parent method has been dropped due to short execution time. */
    public static final int DROPPED_PARENT = 0x0010;

    /** Trace and method info */
    private String traceName, className, methodName, methodSignature;

    /** Wall clock time (set only in records marking beginning of a trace. */
    private long clock;

    /** Execution time (in nanoseconds) */
    private long time;

    /** Number of subordinate method calls (and errors) */
    private long errors, calls;

    /** Record flags and additional information (added by viewer loader) */
    private int flags, level, records;

    /** Trace marker flags */
    private int traceFlags;

    private boolean expanded = true;

    /** Parent record */
    private NamedTraceRecord parent;

    /** Subordinate method call records */
    private List<NamedTraceRecord> children;

    /** Additional attributes */
    private Map<String,Object> attrs;

    /** Exception object (if any) */
    private SymbolicException exception;

    /** Execution time percentage (of whole trace) */
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


    public void setException(SymbolicException exception) {
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


    public boolean hasError() {
        return hasFlag(NamedTraceRecord.EXCEPTION_PASS) || this.getException() != null;
    }


    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
    }


    public boolean isExpanded() {
        return expanded;
    }


    public boolean toggleExpanded() {
        expanded = !expanded;
        return expanded;
    }


    /**
     * Returns attribute value (and guards against null attribute dictionary)
     *
     * @param attrName attribute name
     *
     * @return attribute value
     */
    public Object getAttr(String attrName) {
        return attrs != null ? attrs.get(attrName) : null;
    }


    /**
     * Sets attribute value (and guards against null attribute dictionary)
     *
     * @param attrName attribute name
     *
     * @param attrVal attribute value
     */
    public void setAttr(String attrName, Object attrVal) {
        if (attrs == null) {
            attrs = new HashMap<String,Object>();
        }
        attrs.put(attrName, attrVal);
    }


    /**
     * Returns attribute map (or empty map if attribute dictionary is null)
     *
     * @return attribute map
     */
    public Map<String,Object> getAttrs() {
        return Collections.unmodifiableMap(attrs != null ? attrs : new HashMap<String, Object>());
    }


    /**
     * Returns number of attached attributes
     *
     * @return number of attached attributes
     */
    public int numAttrs() {
        return attrs != null ? attrs.size() : 0;
    }


    /**
     * Adds child record (and creates list of children if null)
     *
     * @param child child record
     */
    public void addChild(NamedTraceRecord child) {
        if (children == null) {
            children = new ArrayList<NamedTraceRecord>();
        }
        children.add(child);
    }


    /**
     * Returns child record (or null if none exists)
     *
     * @param n index
     *
     * @return child trace record
     */
    public NamedTraceRecord getChild(int n) {
        return (children != null && n < children.size()) ? children.get(n) : null;
    }


    /**
     * Returns number of child trace records
     *
     * @return number of child trace records
     */
    public int numChildren() {
        return children != null ? children.size() : 0;
    }


    /**
     * Returns list of child trace records (or empty list if none found)
     *
     * @return list of child trace records
     */
    public List<NamedTraceRecord> getChildren() {
        return Collections.unmodifiableList(children != null ? children : new ArrayList<NamedTraceRecord>(1));
    }

    /** Print short class name */
    public static final int PS_SHORT_CLASS = 0x01;

    /** Print result type */
    public static final int PS_RESULT_TYPE = 0x02;

    /** Print short argument types */
    public static final int PS_SHORT_ARGS  = 0x04;

    /** Omits arguments overall in pretty pring */
    public static final int PS_NO_ARGS     = 0x08;

    /**
     * Returns human readable method description (with default flags)
     *
     * @return method description string
     */
    public String prettyPrint() {
        return prettyPrint(PS_RESULT_TYPE|PS_SHORT_ARGS);
    }


    /**
     * Returns human readable method description (configurable with supplied flags)
     *
     * @param style style flags (see PS_* constants)
     *
     * @return method description string
     */
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


    /**
     * Returns human readable version of clock attribute
     *
     * @return human readable trace begin time
     */
    public String prettyClock() {
        return new SimpleDateFormat("hh:mm:ss.SSS").format(getClock());
    }


    /**
     * Calculates method execution time percentage and recursion level
     * for this record and all children recursively
     *
     * @param total total trace execution time
     *
     * @param level recursion level of parent method
     */
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


    /**
     * Creates flat-list representation of trace record tree.
     *
     * @param result list object to be populated
     */
    public void scanRecords(List<NamedTraceRecord> result, NamedRecordFilter filter) {
        if (filter == null || filter.matches(this)) {
            result.add(this);
        }

        if (children != null && expanded && (filter == null || filter.recurse(this))) {
            for (NamedTraceRecord child : children) {
                child.scanRecords(result, filter);
            }
        }
    }
}
