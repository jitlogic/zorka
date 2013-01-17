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

public class TraceRecord {

    private int traceId;
    private long clock;
    private TraceElement root;

    private long minimumTime = 1;


    public TraceRecord(TraceElement root, int traceId, long clock) {
        this.root = root;
        this.traceId = traceId;
        this.clock = clock;
    }


    public TraceElement getRoot() {
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
}
