/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import java.util.UUID;

import static com.jitlogic.zorka.core.spy.TracerLib.DTRACE_SEP;

public class DTraceState {

    private String uuid;
    private String tid;
    private int seq;
    private long tstart;
    private long threshold;
    private TracerLib tracer;

    public DTraceState(TracerLib tracer, String uuid, String tid, long tstart, long threshold) {
        this.tracer = tracer;
        this.uuid = uuid != null ? uuid : UUID.randomUUID().toString();
        this.tid = tid != null ? tid : "";
        this.tstart = tstart;
        this.threshold = threshold;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTid() {
        return tid;
    }

    public long getTstart() {
        return tstart;
    }

    public long getThreshold() {
        return threshold;
    }

    public synchronized int nextSeq() {
        seq++;
        return seq;
    }

    public synchronized String nextTid() {
        seq++;
        return String.format("%s%s%x", tid, DTRACE_SEP, seq);
    }

    public synchronized String lastTid() {
        return String.format("%s%s%x", tid, DTRACE_SEP, seq);
    }

    @Override
    public String toString() {
        return "DT(" + uuid + ", '" + tid + "', " + threshold + ")";
    }
}
