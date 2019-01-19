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

package com.jitlogic.zorka.core.spy.tuner;


public class TraceTuningStats {

    public static final int STATS_SIZE = 2048;
    public static final int STATS_MASK = STATS_SIZE-1;

    public static final long MID_MASK = 0x00000000FFFFFFFFL;

    public static final long XCL_MIN1 = 6;
    public static final long XCL_MIN2 = 24;

    public static final int IMAX = 24;

    public static final long MMAX = 16;

    private long threadId;
    private long tstamp;
    private long calls;

    private int misses0 = 0;
    private int misses1 = 0;
    private int misses2 = 0;

    private long[] stats;

    public TraceTuningStats() {
        this.stats = new long[STATS_SIZE];
    }

    public void clear() {
        tstamp = calls = 0;
        for (int i = 0; i < STATS_SIZE; i++) stats[i] = 0;
    }

    public int getSize() {
        return STATS_SIZE;
    }

    public long[] getStats() {
        return stats;
    }

    public boolean markRank(int mid, int delta) {

        int x0 = mid & STATS_MASK;

        for (int i = 0; i < IMAX; i++) {
            int x1 = (x0+i) & STATS_MASK;
            long s1 = stats[x1];
            long m1 = s1 & MID_MASK;
            if (m1 == 0 || m1 == mid) {
                long v1 = (s1 >>> 32) + delta;
                stats[x1] = v1 > 0 ? ((v1 << 32) | mid) : 0;
                return true;

            }
        }

        misses0++;
        if (delta <= 0) return true;

        for (int i = 0; i < IMAX; i++) {
            int x1 = (x0+i) & STATS_MASK;
            long s1 = stats[x1];
            long v1 = s1 >>> 32;
            if (v1 < XCL_MIN1) {
                stats[x1] = mid & ((v1+delta) << 32);
                return true;
            }
        }

        misses1++;

        for (int i = 0; i < IMAX; i++) {
            int x1 = (x0+i) & STATS_MASK;
            long s1 = stats[x1];
            long v1 = s1 >>> 32;
            if (v1 < XCL_MIN2) {
                stats[x1] = mid & ((v1+delta) << 32);
                return true;
            }
        }

        misses2++;

        return misses2 < MMAX;
    }

    public long getRank(int mid) {
        return mid < STATS_SIZE ? (stats[mid] & MID_MASK) : 0;
    }

    @Override
    public String toString() {
        int used = 0;

        for (int i = 0; i < STATS_SIZE; i++)
            if (stats[i] != 0) used++;

        return "TS: " + "tid=" + threadId + " calls=" + calls + " size=" + STATS_SIZE + " used=" + used +
                " misses.0=" + misses0 + " misses.1=" + misses1 + " misses.2=" + misses2 + ")";
    }

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

}
