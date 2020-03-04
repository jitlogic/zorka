/*
 * Copyright 2016-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.math.BigInteger;
import java.util.*;

/**
 * Metadata describing given trace chunk. Some attributes are stored in MetadataQuickIndex,
 * other ones are used as interim in trace ingestion process.
 */
public class TraceChunkData {

    private TraceChunkData parent;

    /** Trace ID (high word) */
    private long traceId1;

    /** Trace ID (low word) */
    private long traceId2;

    /** Span ID */
    private long spanId;

    /** Chunk sequential number. */
    private int chunkNum;

    private int tsNum;

    /** Parent ID */
    private long parentId;

    /** Trace flags. See com.jitlogic.zorka.common.cbor.TraceRecordFlags for reference. */
    private int tflags;

    /** Trace timestamp (time when trace started in milliseconds since Epoch). */
    private long tstamp;

    /** Trace duration (in seconds). */ // TODO milliseconds/microseconds/ticks
    private long duration;

    /** Start offset of trace inside data chunk. For top level traces it should always be 0. */
    private int startOffs;

    /** Initial stack depth (should be 0 for top level traces, more than 0 for all embedded traces). */
    private int stackDepth;

    private int ttypeId;

    private String ttype;

    /** Number of method calls processed (but not always recorded) by tracer. */
    private int calls;

    /** Number of errors registered by tracer. */
    private int errors;

    /** Number of method calls recorded by tracer. */
    private int recs;

    /** Chunk start timestamp (nanoseconds since trace start). */
    private long tstart = Long.MIN_VALUE;

    /** Chunk end timestamp (nanoseconds since trace start). */
    private long tstop = Long.MAX_VALUE;

    /** Trace data (CBOR encoded and compressed) */
    private byte[] traceData;

    /** Symbol data (CBOR encoded and compressed) - used to retrieve trace in human-readable form */
    private byte[] symbolData;

    /** String attributes */
    private Map<String,String> attrs;

    /** Root method of trace call tree - class name */
    private String klass;

    /** Root method of trace call tree - method name */
    private String method;

    /** List of all methods found in this chunk */
    private Set<Integer> methods;

    private TraceDataResultException exception;

    private volatile int size = -1;

    @Override
    public String toString() {
        return "ChunkMetadata(" + getMethod() +  " tid=" + getTraceIdHex() + ",sid=" + getSpanIdHex() + ",chn=" + getChunkNum()
            + ",pid=" + getParentIdHex() + ")";
    }

    public TraceChunkData(String traceid, String spanid, String parentid, Integer chnum) {
        if (traceid.length() == 16) {
            traceId1 = new BigInteger(traceid, 16).longValue();
            traceId2 = 0L;
        } else if (traceid.length() == 32) {
            traceId1 = new BigInteger(traceid.substring(0, 16), 16).longValue();
            traceId2 = new BigInteger(traceid.substring(16,32), 16).longValue();
        }
        spanId = new BigInteger(spanid, 16).longValue();
        if (parentid != null) parentId = new BigInteger(parentid, 16).longValue();
        if (chnum != null) chunkNum = chnum;
    }

    public TraceChunkData(long traceId1, long traceId2, long parentId, long spanId, int chunkNum) {
        this.traceId1 = traceId1;
        this.traceId2 = traceId2;
        this.parentId = parentId;
        this.chunkNum = chunkNum;
        this.spanId = spanId;
    }

    public int size() {
        if (size == -1) {
            synchronized (this) {
                int rslt = 256 + (traceData != null ? traceData.length : 0);
                if (attrs != null) {
                    for (Map.Entry<String,String> e : attrs.entrySet()) {
                        rslt += 64 + (e.getKey() != null ? e.getKey().length() : 0) + (e.getValue() != null ? e.getValue().length() : 0);
                    }
                }
                this.size = rslt;
            }
        }
        return size;
    }

    public TraceChunkData getParent() {
        return parent;
    }

    public void setParent(TraceChunkData parent) {
        this.parent = parent;
    }

    public long getTraceId1() {
        return traceId1;
    }

    public void setTraceId1(long traceId1) {
        this.traceId1 = traceId1;
    }

    public long getTraceId2() {
        return traceId2;
    }

    public void setTraceId2(long traceId2) {
        this.traceId2 = traceId2;
    }

    public String getTraceIdHex() {
        return traceId2 != 0 ? ZorkaUtil.hex(traceId1, traceId2) : ZorkaUtil.hex(traceId1);
    }

    public long getSpanId() {
        return spanId;
    }

    public String getSpanIdHex() {
        return spanId != 0 ? ZorkaUtil.hex(spanId) : null;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public long getParentId() {
        return parentId;
    }

    public String getParentIdHex() {
        return parentId != 0 ? ZorkaUtil.hex(parentId) : null;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public int getFlags() {
        return tflags;
    }

    public void setFlags(int tflags) {
        this.tflags = tflags;
    }

    public long getTstamp() {
        return tstamp;
    }

    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }

    public long catchTstamp(long tstamp) {
        this.tstamp = tstamp;
        return tstamp;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public long catchDuration(long duration) {
        this.duration = duration;
        return duration;
    }

    public boolean hasFlag(int flag) {
        return flag == (tflags & flag);
    }

    public void markFlag(int flag) {
        tflags |= flag;
    }

    public void clearFlag(int flag) {
        tflags &= ~flag;
    }

    public boolean hasError() {
        return 0 != (tflags & TraceMarker.ERROR_MARK);
    }

    public void setError(boolean errorFlag) {
        if (errorFlag) {
            tflags |= TraceMarker.ERROR_MARK;
        } else {
            tflags &= ~TraceMarker.ERROR_MARK;
        }
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public void setChunkNum(int chunkNum) {
        this.chunkNum = chunkNum;
    }

    public int getTsNum() {
        return tsNum;
    }

    public void setTsNum(int tsNum) {
        this.tsNum = tsNum;
    }

    public int getStartOffs() {
        return startOffs;
    }

    public void setStartOffs(int startOffs) {
        this.startOffs = startOffs;
    }

    public int getStackDepth() {
        return stackDepth;
    }

    public void setStackDepth(int stackDepth) {
        this.stackDepth = stackDepth;
    }

    public int getTtypeId() {
        return ttypeId;
    }

    public void setTtypeId(int ttypeId) {
        this.ttypeId = ttypeId;
    }

    public String getTtype() {
        return ttype;
    }

    public void setTtype(String ttype) {
        this.ttype = ttype;
    }

    public int getCalls() {
        return calls;
    }

    public void addCalls(int calls) {
        this.calls = Math.max(this.calls, calls);
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }

    public int getErrors() {
        return errors;
    }

    public void addErrors(int errors) {
        this.errors += errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getRecs() {
        return recs;
    }

    public void addRecs(int recs) {
        this.recs += recs;
    }

    public void setRecs(int recs) {
        this.recs = recs;
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

    public Map<String,String> getAttrs() {
        if (attrs == null) attrs = new HashMap<String,String>();
        return attrs;
    }

    public String getAttr(String attrName) {
        return attrs != null ? attrs.get(attrName) : null;
    }

    public void setAttr(String attrName, String attrVal) {
        if (attrs == null) attrs = new HashMap<String, String>();
        attrs.put(attrName, attrVal);
    }

    public byte[] getTraceData() {
        return traceData;
    }

    public void setTraceData(byte[] traceData) {
        this.traceData = traceData;
    }

    public byte[] getSymbolData() {
        return symbolData;
    }

    public void setSymbolData(byte[] symbolData) {
        this.symbolData = symbolData;
    }

    public TraceDataResultException getException() {
        return exception;
    }

    public void setException(TraceDataResultException exception) {
        this.exception = exception;
    }

    public Set<Integer> getMethods() {
        if (methods == null) methods = new HashSet<Integer>();
        return methods;
    }

    public void addMethod(int methodId) {
        if (methods == null) methods = new HashSet<Integer>();
        methods.add(methodId);
    }

    public String getKlass() {
        return klass;
    }

    public void setKlass(String klass) {
        this.klass = klass;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
