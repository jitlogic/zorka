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

/**
 * Trace marker object marks beginning of a new trace. It can be attached
 * to current trace record at the beginning of a trace.
 */
public class TraceMarker implements SymbolicRecord, Serializable {

    /**
     * Minimum trace execution time required to further process trace
     */
    private static long minTraceTime = 50000000;

    /**
     * Overflow flag (set if some records in current trace have been skipped).
     */
    public static final int OVERFLOW_FLAG = 0x01;

    /**
     * Always submit trace (regardless of trace execution time etc.).
     */
    public static final int SUBMIT_TRACE = 0x02;

    public static final int ALL_METHODS = 0x04;

    /**
     * This flag instructs trace builder algorithm to drop frames whose execution time is too small
     */
    public static final int DROP_INTERIM = 0x08;

    /**
     * This flag instructs trace builder to log all method calls (on entry and on return/error)
     */
    public static final int TRACE_CALLS = 0x10;

    /**
     * Always drop trace (regardless of trace execution time etc.).
     */
    public static final int DROP_TRACE = 0x20;

    /**
     * Trace error mark. This flag is used indicate that this trace ended with error condition (eg. HTTP/500).
     */
    public static final int ERROR_MARK = 0x1000;

    /**
     * Trace ID (refers to symbol containing trace name)
     */
    private int traceId;

    /**
     * Trace start (wall clock time)
     */
    private long clock;

    /**
     * Minimum execution time of this trace
     */
    private long minimumTime;

    /**
     * Trace marker flags
     */
    private int flags;


    public static long getMinTraceTime() {
        return minTraceTime;
    }


    public static void setMinTraceTime(long traceTime) {
        minTraceTime = traceTime;
    }


    /**
     * Creates new trace marker.
     *
     * @param root    root trace record
     * @param traceId trace ID
     * @param clock   current time (wall clock time)
     */
    public TraceMarker(TraceRecord root, int traceId, long clock) {
        this(traceId, clock);
        TraceMarker parent = root.getMarker();
        if (parent != null) {
            this.minimumTime = parent.getMinimumTime();
            this.flags = parent.getFlags();
        } else {
            this.minimumTime = getMinTraceTime();
            this.flags = 0;
        }
    }


    public TraceMarker(int traceId, long clock) {
        this.traceId = traceId;
        this.clock = clock;
    }


    public int getTraceId() {
        return traceId;
    }

    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }


    public long getClock() {
        return clock;
    }


    public void setClock(long clock) {
        this.clock = clock;
    }


    public long getMinimumTime() {
        return minimumTime;
    }


    public void setMinimumTime(long minimumTime) {
        this.minimumTime = minimumTime;
    }


    public int getFlags() {
        return flags;
    }


    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
    }


    public void setFlags(int flags) {
        this.flags = flags;
    }


    public void inheritFlags(int flags) {
        this.flags |= flags & OVERFLOW_FLAG;
    }


    public void markFlags(int flag) {
        this.flags |= flag;
    }


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        traceId = checker.checkSymbol(traceId, this);
    }

    @Override
    public String toString() {
        return "TraceMarker(" + traceId + ")";
    }
}
