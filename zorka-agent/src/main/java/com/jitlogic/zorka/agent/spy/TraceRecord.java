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

package com.jitlogic.zorka.agent.spy;

import com.jitlogic.zorka.common.SymbolRegistry;
import com.jitlogic.zorka.common.SymbolicException;
import com.jitlogic.zorka.common.TraceEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents trace information about single method execution.
 * May contain references to information about calls from this method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceRecord {

    /** Overflow record will be discarded regardless of method execution time and other conditions. */
    public static final int OVERFLOW_FLAG = 0x0001;

    /** Indicates that new trace has been started from here. */
    public static final int TRACE_BEGIN   = 0x0002;

    /** Exception thrown from method called from frame hasn't been handled and has been thrown out of current frame. */
    public static final int EXCEPTION_PASS = 0x0004;

    /** Exception thrown from method called current frame has been wrapped and thrown out of current frame. */
    public static final int EXCEPTION_WRAP = 0x0008;

    /** Class ID refers to class name in symbol registry. */
    private int classId;

    /** Method ID refers to method name in symbol registry. */
    private int methodId;

    /** Signature ID refers to signature string in symbol registry. */
    private int signatureId;

    /** Various flags (see *_FLAG constants defined above) */
    private int flags;

    /** Method execution time. */
    private long time;

    /** Number of instrumented method calls performed from this method (and recursively from all subsequent calls. */
    private long calls;

    /** Number of (catched) errors found in  */
    private long errors;

    /** If not null, this reference contains trace marker (that identifies and delimits traces).
     *  Tracer can submit trace when exiting from method marked by trace marker. */
    private TraceMarker marker;

    /** Exception caught on method exit (if any) */
    private Object exception;

    /** Parent trace record represents information about method execution from which current method execution was called. */
    private TraceRecord parent;

    /** Attributes grabbed at this method execution (by spy instrumentation engine). */
    private Map<Integer,Object> attrs;

    /** Contains list of records describing method executions called directly from this execution. */
    private List<TraceRecord> children;

    /**
     * Creates trace record.
     *
     * @param parent parent record
     */
    public TraceRecord(TraceRecord parent) {
        this.parent = parent;

        if (parent != null) {
            this.marker = parent.getMarker();
        }
    }


    /**
     * Returns custom attribute value.
     *
     * @param attrId ID referring to symbol name from symbol registry
     *
     * @return attribute value or null if no attribute has been found
     */
    public Object getAttr(int attrId) {
        if (attrs != null) {
            return attrs.get(attrId);
        } else {
            return null;
        }
    }


    /**
     * Sets custom attribute value.
     *
     * @param attrId ID referring to symbol name from symbol registry
     *
     * @param attrVal attribute value
     */
    public void setAttr(int attrId, Object attrVal) {
        if (attrs == null) {
            attrs = new HashMap<Integer,Object>();
        }
        attrs.put(attrId, attrVal);
    }


    /**
     * Attaches trace record of method execution called from current execution (i.e.
     * described by this record).
     *
     * @param child child trace record
     */
    public void addChild(TraceRecord child) {
        if (children == null) {
            children = new ArrayList<TraceRecord>();
        }
        children.add(child);
        child.parent = this;
    }


    /**
     * Returns child record.
     *
     * @param i child record index (starting with 0)
     *
     * @return child trace record.
     */
    public TraceRecord getChild(int i) {
        if (children != null && i < children.size()) {
            return children.get(i);
        } else {
            return null;
        }
    }


    /**
     * Return number of child trace records stored by current record.
     *
     * @return number of records
     */
    public int numChildren() {
        return children != null ? children.size() : 0;
    }


    public TraceRecord getParent() {
        return parent;
    }


    public long getCalls() {
        return calls;
    }


    public void setCalls(long calls) {
        this.calls = calls;
    }


    public long getErrors() {
        return errors;
    }


    public void setErrors(long errors) {
        this.errors = errors;
    }


    public int getClassId() {
        return classId;
    }


    public void setClassId(int classId) {
        this.classId = classId;
    }


    public int getMethodId() {
        return methodId;
    }


    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }


    public int getSignatureId() {
        return signatureId;
    }


    public void setSignatureId(int signatureId) {
        this.signatureId = signatureId;
    }


    public long getTime() {
        return time;
    }


    public void setTime(long time) {
        this.time = time;
    }


    public Object getException() {
        return exception;
    }


    public void setException(Object exception) {
        this.exception = exception;
    }


    public TraceMarker getMarker() {
        return marker;
    }


    public void setMarker(TraceMarker marker) {
        this.marker = marker;
    }


    /**
     * Returns wall clock time of method execution represented by this record.
     * Note that wall clock time is set only for records with trace markers
     * (that is, beginning of a trace). This is for performance reasons.
     * In practice this means that this value refers to exact time when first
     * method of recorded trace has been called. Zero is returned otherwise.
     *
     * @return wall clock time of method execution
     */
    public long getClock() {
        return marker != null ? marker.getClock() : 0L;
    }


    /**
     * Returns trace ID of method execution represented by this method.
     * Trace ID is set only for records with trace markers attached.
     *
     * @return
     */
    public int getTraceId() {
        return marker != null ? marker.getTraceId() : 0;
    }


    /**
     * Traverses record (and recursively all its children) and
     * sends its content as series of calls to supplied trace
     * event handler object.
     *
     * @param output output handler object
     */
    public void traverse(TraceEventHandler output) {

        if (hasFlag(TRACE_BEGIN)) {
            output.traceBegin(marker.getTraceId(), getClock(), marker != null ? marker.getFlags() : 0);
        }

        output.traceEnter(classId, methodId, signatureId, 0);
        output.traceStats(calls, errors, flags);

        if (attrs != null) {
            for (Map.Entry<Integer,Object> entry : attrs.entrySet()) {
                output.newAttr(entry.getKey(), entry.getValue());
            }
        }

        if (children != null) {
            for (TraceRecord child : children) {
                child.traverse(output);
            }
        }

        if (exception != null) {
            output.traceError(exception, time);
        } else {
            output.traceReturn(time);
        }
    }


    /**
     * Cleans up record for reuse. This is used to limit amount of
     * memory consumed by subsequent allocations (thus imposing less
     * strain on gabage collection).
     *
     * Trace records are not actually pooled in any way, but can often
     * be reused while tracing subsequent method calls.
     */
    public void clean() {
        time = 0;
        classId = methodId = signatureId = 0;
        attrs = null;
        children = null;
        marker = null;
        calls = errors = 0;
        flags = 0;
    }

    public void markFlag(int flag) {
        flags |= flag;
    }

    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
    }


    /**
     * Returns true if record is part of recorded trace.
     * This method is implemented for readability purposes,
     * so algorithmic code like TraceBuilder will be easier
     * to understand. Do not factor it out.
     *
     * @return true if record is part of recorded trace
     */
    public boolean inTrace() {
        return marker != null;
    }

    /**
     * Returns true if record is empty (no actual frame has been
     * recorded in it). This method is implemented for readability
     * purposes, so algorithmic code like TraceBuilder will be easier
     * to understand. Do not factor it out.
     *
     * @return true if record is empty
     */
    public boolean isEmpty() {
        return classId == 0;
    }

    public void fixup(SymbolRegistry symbols) {

        if (children != null) {
            for (TraceRecord child : children) {
                child.fixup(symbols);
            }
        }

        if (exception instanceof Throwable) {
            exception = new SymbolicException((Throwable)exception, symbols, 0 == (flags & EXCEPTION_WRAP));
        }
    }
}
