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

package com.jitlogic.zorka.core.spy;


import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents distribute trace context. Note that this is now central context for tracer, even in single-host mode.
 */
public class DTraceState {

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

    private String traceState;

    public DTraceState(long traceId1, long traceId2, long parentId, long spanId, long tstart, int flags) {
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
        return ZorkaUtil.hex(spanId);
    }

    public long getParentId() {
        return parentId;
    }

    public String getParentIdHex() {
        return ZorkaUtil.hex(parentId);
    }

    public long getTstart() {
        return tstart;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getDebugId() {
        return debugId;
    }

    public DTraceState setDebugId(String debugId) {
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
        if (obj instanceof DTraceState) {
            DTraceState ds = (DTraceState)obj;
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
