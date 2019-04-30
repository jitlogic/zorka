/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents distribute trace context. Note that this is now central context for tracer, even in single-host mode.
 */
public class DTraceContext {

    /** Trace ID (64 or 128 bit) */
    private long traceId1, traceId2;

    /** Span ID and Parent Span ID */
    private long spanId, parentId;

    /** Start timestamp */
    private long tstart;

    /** Context Flags */
    private int flags;

    /** Debug ID (if any) */
    private String debugId;

    /** Baggage */
    private Map<String,String> baggage = new TreeMap<String, String>();

    /** W3C Trace State */
    private String traceState;

    public DTraceContext(DTraceContext orig) {
        this.traceId1 = orig.traceId1;
        this.traceId2 = orig.traceId2;
        this.parentId = orig.parentId;
        this.spanId = orig.spanId;
        this.tstart = orig.tstart;
        this.flags = orig.flags;
        this.debugId = orig.debugId;
        this.baggage = orig.baggage;
        this.traceState = orig.traceState;
    }

    public DTraceContext(long traceId1, long traceId2, long parentId, long spanId, long tstart, int flags) {
        this.traceId1 = traceId1;
        this.traceId2 = traceId2;
        this.parentId = parentId;
        this.flags = flags;

        this.spanId = spanId;
        this.tstart = tstart;
    }

    public long getTraceId1() {
        return traceId1;
    }

    public long getTraceId2() {
        return traceId2;
    }

    public String getTraceIdHex() {
        return traceId2 != 0 ? ZorkaUtil.hex(traceId1, traceId2) : ZorkaUtil.hex(traceId1);
    }

    public long getSpanId() {
        return spanId;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public String getSpanIdHex() {
        return spanId != 0 ? ZorkaUtil.hex(spanId) : null;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getParentIdHex() {
        return parentId != 0 ? ZorkaUtil.hex(parentId) : null;
    }

    public long getTstart() {
        return tstart;
    }

    public void setTstart(long tstart) {
        this.tstart = tstart;
    }

    public synchronized void markFlags(int flags) {
        this.flags |= flags;
    }

    public synchronized boolean hasFlags(int flags) {
        return 0 != (this.flags & flags);
    }

    public synchronized int getFlags() {
        return flags;
    }

    public synchronized void setFlags(int flags) {
        this.flags = flags;
    }

    public String getDebugId() {
        return debugId;
    }

    public DTraceContext setDebugId(String debugId) {
        this.debugId = debugId;
        return this;
    }

    public Map<String, String> getBaggage() {
        return baggage;
    }

    public String getTraceState() {
        return traceState;
    }

    public void setTraceState(String traceState) {
        this.traceState = traceState;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DTraceContext) {
            DTraceContext ds = (DTraceContext)obj;
            return ds.traceId1 == traceId1 &&
                    ds.traceId2 == traceId2 &&
                    ds.spanId == spanId &&
                    ds.parentId == parentId &&
                    ds.tstart == tstart &&
                    ds.flags == flags;
        } else return false;
    }

    @Override
    public int hashCode() {
        return (int) (11 * traceId1 + 17 * traceId2 + 31 * spanId + 41 * parentId + 7 * tstart + 3 * flags);
    }

    @Override
    public String toString() {
        return String.format("DT(%016x%016x,%016x,%02x,t=%d)", traceId1, traceId2, spanId, flags, tstart);
    }
}
