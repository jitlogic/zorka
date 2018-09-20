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

package com.jitlogic.zorka.core.spy.tuner;

/**
 * Summary stats data is periodically passed to tracer tuner.
 */
public class TraceSummaryStats {

    private long threadId;
    private long tstamp;
    private long calls;

    private TraceDetailStats details = new TraceDetailStats();

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getTstamp() {
        return tstamp;
    }

    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public TraceDetailStats getDetails() {
        return details;
    }

    public void setDetails(TraceDetailStats details) {
        this.details = details;
    }

    public void clear() {
        tstamp = calls = 0;
        if (details != null) details.clear();
    }

    @Override
    public String toString() {

        return "TSS(id=" + threadId + ",t=" + tstamp + ",calls=" + calls+ ", det=" + details +")";
    }
}
