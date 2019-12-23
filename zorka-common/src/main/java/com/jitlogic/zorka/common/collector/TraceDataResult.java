package com.jitlogic.zorka.common.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents extracted trace record. This is roughly equivalent to TraceRecord but
 * designed for using to present data, not for compactness. All symbols, methods and
 * exceptions are resolved and in human readable form.
 */
public class TraceDataResult {

    /** Class and method (human-readable form) */
    private String method;

    /** Flags (error flag) */
    private int flags;

    /** Start time (millis since Epoch) */
    private long tstamp;

    /** Trace start (nanos since JVM start) */
    private long tstart;

    /** Trace stop (nanos since JVM start) */
    private long tstop;

    /** Span ID */
    private long spanId;

    /** Parent ID */
    private long parentId;

    /** Position in CBOR data stream */
    private int chunkPos;

    /** Length CBOR data stream */
    private int chunkLen;

    /** Number of registered calls */
    private long calls;

    /** Number of registered records */
    private long recs;

    /** Number of errors */
    private long errors;

    private String traceType;

    private TraceDataResult parent;

    /** Exception (if any) */
    private TraceDataResultException exception;

    /** Attributes (if any) */
    private Map<String,String> attributes;

    /** Child records (if any) */
    private List<TraceDataResult> children;

    public TraceDataResult(TraceDataResult parent) {
        this.parent = parent;
        if (parent != null) this.parent.addChild(this);
    }

    public void addChild(TraceDataResult child) {
        if (children == null) children = new ArrayList<TraceDataResult>();
        children.add(child);
    }

    public void setAttr(String k, String v) {
        if (attributes == null) attributes = new HashMap<String, String>();
        attributes.put(k,v);
    }

    public String getAttr(String k) {
        return attributes.get(k);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public long getTstamp() {
        return tstamp;
    }

    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }

    public long getTstart() {
        return tstart;
    }

    public void setTstart(long tstart) {
        this.tstart = tstart;
    }

    public long getTstop() {
        return tstop;
    }

    public void setTstop(long tstop) {
        this.tstop = tstop;
    }

    public long getSpanId() {
        return spanId;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public long getChunkPos() {
        return chunkPos;
    }

    public void setChunkPos(int chunkPos) {
        this.chunkPos = chunkPos;
    }

    public long getChunkLen() {
        return chunkLen;
    }

    public void setChunkLen(int chunkLen) {
        this.chunkLen = chunkLen;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public long getRecs() {
        return recs;
    }

    public void setRecs(long recs) {
        this.recs = recs;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }

    public TraceDataResult getParent() {
        return parent;
    }

    public void setParent(TraceDataResult parent) {
        this.parent = parent;
    }

    public TraceDataResultException getException() {
        return exception;
    }

    public void setException(TraceDataResultException exception) {
        this.exception = exception;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public List<TraceDataResult> getChildren() {
        return children;
    }

    public void setChildren(List<TraceDataResult> children) {
        this.children = children;
    }
}
