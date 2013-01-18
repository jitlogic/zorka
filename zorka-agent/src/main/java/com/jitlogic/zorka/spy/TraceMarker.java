/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.spy;

public class TraceMarker {

    public static final int OVERFLOW_FLAG = 0x01;

    private int traceId;
    private long clock;

    private TraceMarker parent;
    private TraceRecord root;

    private long minimumTime;
    private int flags;


    public TraceMarker(TraceMarker parent, TraceRecord root, int traceId, long clock) {
        this.parent = parent;
        this.root = root;
        this.traceId = traceId;
        this.clock = clock;
        this.minimumTime = Tracer.getDefaultTraceTime();
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


    public void markOverflow() {
        flags |= OVERFLOW_FLAG;
    }


    public TraceMarker pop() {
        if (parent != null) {
            parent.flags = flags;
            return parent;
        } else {
            return null;
        }
    }
}
