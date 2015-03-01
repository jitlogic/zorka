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

package com.jitlogic.zorka.common.tracedata;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;


/**
 * Represents trace information about single method execution.
 * May contain references to information about calls from this method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceRecord implements SymbolicRecord, Serializable {


    /**
     * Overflow record will be discarded regardless of method execution time and other conditions.
     */
    public static final int OVERFLOW_FLAG = 0x0001;

    /**
     * Indicates that new trace has been started from here.
     */
    public static final int TRACE_BEGIN = 0x0002;

    /**
     * Exception thrown from method called from frame hasn't been handled and has been thrown out of current frame.
     */
    public static final int EXCEPTION_PASS = 0x0004;

    /**
     * Exception thrown from method called current frame has been wrapped and thrown out of current frame.
     */
    public static final int EXCEPTION_WRAP = 0x0008;

    /**
     * Indicates that parent method has been dropped due to short execution time.
     */
    public static final int DROPPED_PARENT = 0x0010;

    /**
     * Class ID refers to class name in symbol registry.
     */
    private int classId;

    /**
     * Method ID refers to method name in symbol registry.
     */
    private int methodId;

    /**
     * Signature ID refers to signature string in symbol registry.
     */
    private int signatureId;

    /**
     * Various flags (see *_FLAG constants defined above)
     */
    private int flags;

    /**
     * Method execution time.
     */
    private long time;

    /**
     * Number of instrumented method calls performed from this method (and recursively from all subsequent calls.
     */
    private long calls;

    /**
     * Number of (catched) errors found in
     */
    private long errors;

    /**
     * If not null, this reference contains trace marker (that identifies and delimits traces).
     * Tracer can submit trace when exiting from method marked by trace marker.
     */
    private TraceMarker marker;

    /**
     * Exception caught on method exit (if any)
     */
    private Object exception;

    /**
     * Parent trace record represents information about method execution from which current method execution was called.
     */
    private TraceRecord parent;

    /**
     * Attributes grabbed at this method execution (by spy instrumentation engine).
     */
    private Map<Integer, Object> attrs;

    /**
     * Contains list of records describing method executions called directly from this execution.
     */
    private List<TraceRecord> children;


    public TraceRecord() {
    }

    /**
     * Creates trace record.
     *
     * @param parent parent record
     */
    public TraceRecord(TraceRecord parent) {
        setParent(parent);
    }


    /**
     * Creats shallow copy of trace record object.
     *
     * @return copy of trace record
     */
    public TraceRecord copy() {
        TraceRecord tr = new TraceRecord();
        tr.classId = classId;
        tr.methodId = methodId;
        tr.signatureId = signatureId;
        tr.flags = flags;
        tr.time = time;
        tr.calls = calls;
        tr.errors = errors;
        tr.marker = marker;
        tr.exception = exception;
        tr.parent = parent;
        tr.attrs = attrs;
        tr.children = children;
        return tr;
    }


    /**
     * Sets new parent for this method. Current record 'inherits'
     * trace marker of its parent.
     *
     * @param parent parent record
     */
    public void setParent(TraceRecord parent) {
        this.parent = parent;

        if (parent != null) {
            this.marker = parent.getMarker();
        }
    }


    /**
     * Returns custom attribute value.
     *
     * @param attrId ID referring to symbol name from symbol registry
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
     * Returns number of custom attributes attached to this method via Spy.
     *
     * @return number of attributes
     */
    public int numAttrs() {
        return attrs != null ? attrs.size() : 0;
    }


    public Map<Integer, Object> getAttrs() {
        return attrs;
    }


    public void setAttrs(Map<Integer, Object> attrs) {
        this.attrs = attrs;
    }

    /**
     * Sets custom attribute value.
     *
     * @param attrId  ID referring to symbol name from symbol registry
     * @param attrVal attribute value
     */
    public void setAttr(int attrId, Object attrVal) {
        if (attrs == null) {
            attrs = new LinkedHashMap<Integer, Object>();
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


    public List<TraceRecord> getChildren() {
        return children;
    }


    public void setChildren(List<TraceRecord> children) {
        this.children = children;
    }


    public TraceRecord getParent() {
        return parent;
    }


    /**
     * Returns number of calls registered by tracer when executing
     * recorded method. This includes method described by this trace
     * record, so it is equal or greater than 1.
     *
     * @return number of calls
     */
    public long getCalls() {
        return calls;
    }


    public void setCalls(long calls) {
        this.calls = calls;
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }


    /**
     * Returns number of exceptions thrown and registered by tracer
     * when executing method recorded by this trace record.
     *
     * @return number of errors
     */
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


    /**
     * If method recorded by this trace has thrown an exception,
     * looks for exception. Traverses method call tree if needed
     * as exception can be actually thrown by another method and
     * tracer would avoid storing copy of exception in this particular
     * record.
     * <p/>
     * Note that this is mainly used in collector/viewer, not agent.
     * It expects that exceptions will be already converted to symbolic form,
     * so it is useful only after trace has been finished and submitted.
     *
     * @return exception object
     */
    public SymbolicException findException() {

        if (exception instanceof SymbolicException) {
            return (SymbolicException) exception;
        } else if (hasFlag(EXCEPTION_PASS) && children != null) {
            return children.get(children.size() - 1).findException();
        }

        return null;
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


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        classId = checker.checkSymbol(classId, this);
        methodId = checker.checkSymbol(methodId, this);
        signatureId = checker.checkSymbol(signatureId, this);

        if (exception instanceof SymbolicException) {
            ((SymbolicException) exception).traverse(checker);
        }

        if (attrs != null) {
            Map<Integer, Object> newAttrs = new LinkedHashMap<Integer, Object>();
            for (Map.Entry<Integer, Object> e : attrs.entrySet()) {
                newAttrs.put(checker.checkSymbol(e.getKey(), this), e.getValue());
            }
            attrs = newAttrs;
        }

        if (children != null) {
            for (TraceRecord child : children) {
                child.traverse(checker);
            }
        }

        if (marker != null && 0 != (flags & TRACE_BEGIN)) {
            marker.traverse(checker);
        }
    }


    /**
     * Cleans up record for reuse. This is used to limit amount of
     * memory consumed by subsequent allocations (thus imposing less
     * strain on gabage collection).
     * <p/>
     * Trace records are not actually pooled in any way, but can often
     * be reused while tracing subsequent method calls.
     */
    public void clean() {
        time = 0;
        classId = methodId = signatureId = 0;
        attrs = null;
        children = null;
        marker = parent != null ? parent.getMarker() : null;
        calls = errors = 0;
        flags = 0;
        exception = null;
    }


    /**
     * Enabled additional flag bits.
     *
     * @param flag flag bits to enable
     */
    public void markFlag(int flag) {
        flags |= flag;
    }


    /**
     * Returns true if any flag bits from argument is enabled in this record.
     *
     * @param flag
     * @return
     */
    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
    }


    public int getFlags() {
        return flags;
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


    /**
     * Traverses through call tree and converts all exception objects into symbolic forms.
     * This operation is performed just before submitting tracer to output (file or collector).
     *
     * @param symbols agent's symbol registry
     */
    public void fixup(SymbolRegistry symbols) {

        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                children.get(i).fixup(symbols);
            }
        }

        if (exception instanceof Throwable) {
            exception = new SymbolicException((Throwable) exception, symbols, 0 == (flags & EXCEPTION_WRAP));
        }
    }


    /**
     * Returns true if method can be dropped if - as interim method - had too
     * short execution time.
     *
     * @return true if this method can be dropped from trace
     */
    public boolean isInterimDroppable() {
        return exception == null
                && attrs == null
                && 0 == (flags & TRACE_BEGIN)
                && children != null && children.size() == 1;
    }

    @Override
    public String toString() {
        return "TraceRecord(classId=" + classId + ", methodId=" + methodId + ", " + marker + ")";
    }
}
