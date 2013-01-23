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

/**
 * Trace marker object marks beginning of a new trace. It can be attached
 * to current trace record at the beginning of a trace.
 */
public class TraceMarker {

    /** Overflow flag (set if some records in current trace have been skipped). */
    public static final int OVERFLOW_FLAG = 0x01;

    /** Trace ID (refers to symbol containing trace name) */
    private int traceId;

    /** Trace start (wall clock time) */
    private long clock;

    /** Parent marker (if any) */
    private TraceMarker parent;

    /** Root record of this trace. */
    private TraceRecord root;

    /** Minimum execution time of this trace */
    private long minimumTime;

    /** Trace marker flags */
    private int flags;


    /**
     * Creates new trace marker.
     *
     * @param parent parent marker (if any)
     *
     * @param root root record
     *
     * @param traceId trace ID
     *
     * @param clock current time (wall clock time)
     */
    public TraceMarker(TraceMarker parent, TraceRecord root, int traceId, long clock) {
        this.parent = parent;
        this.root = root;
        this.traceId = traceId;
        this.clock = clock;
        this.minimumTime = Tracer.getMinTraceTime();
        this.flags = parent != null ? parent.getFlags() : 0;
    }


    public TraceMarker getParent() {
        return parent;
    }


    public TraceRecord getRoot() {
        return root;
    }


    public int getTraceId() {
        return traceId;
    }


    public long getClock() {
        return clock;
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

    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * Sets OVERFLOW flag.
     */
    public void markOverflow() {
        flags |= OVERFLOW_FLAG;
    }


    /**
     * Used by trace builder when it removes marker from top of marker stack.
     * In addition to returning reference to parent marker, it also transfers
     * relevant data from removed marker to its parent marker (notably flags).
     *
     * @return parent marker or null.
     */
    public TraceMarker pop() {
        if (parent != null) {
            parent.flags = flags;
            return parent;
        } else {
            return null;
        }
    }
}
