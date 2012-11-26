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

package com.jitlogic.zorka.rankproc;

/**
 * Bucket aggregate is
 */
public class SlidingBucketAggregate implements BucketAggregate {

    private long base, total, tstart, tlast;
    private int[] stages, offsets;

    private long[] data, windows, tstamps;


    public SlidingBucketAggregate(long base, int... stages) {
        this.tstart = -1;
        this.base = base;

        this.stages = new int[stages.length+1];

        this.stages[0] = 1;
        System.arraycopy(stages, 0, this.stages, 1, stages.length);

        this.total = 0;

        windows = new long[this.stages.length];
        windows[0] = this.base;
        offsets = new int[this.stages.length];
        offsets[0] = 0;
        tstamps = new long[this.stages.length];
        tstamps[0] = -1;

        int dlen = 1;

        for (int i = 1; i < this.stages.length; i++) {
            windows[i] = windows[i-1] * this.stages[i];
            offsets[i] = offsets[i-1] + this.stages[i-1];
            tstamps[i] = -1;
            dlen += this.stages[i];
        }

        data = new long[dlen];
    }


    public int size() {
        return stages.length;
    }


    public long getWindow(int stage) {
        return windows[stage];
    }

    public int getStage(long window) {
        for (int i = 0; i < windows.length; i++) {
            if (windows[i] == window) {
                return i;
            }

            if (windows[i] > window) {
                return (i > 0 && window-windows[i-1] < windows[i]-window) ? i-1 : i;
            }
        }

        return window < windows[windows.length-1] * 2 ? windows.length-1 : -1;
    }


    public long getTime() {
        return tlast-tstart;
    }


    public long getLast() {
        return tlast;
    }


    public long getTotal() {
        return total;
    }


    public long getStart() {
        return tstart;
    }


    public void feed(long tstamp, long val) {

        if (tstart < 0) {
            tstart = tstamp - (tstamp % base);
            tstamps[0] = tstart;
        }

        while (tstamp-tstamps[0] > base) {
            shift(tstamps[0], data[0], 1);
            tstamps[0] += base;
            data[0] = 0;
        }

        data[0] += val;
        total += val;

        tlast = tstamp;
    }


    private void shift(long tstamp, long nval, int stage) {
        int len = stages[stage], idx1 = offsets[stage], idx2 = idx1 + len - 1;
        long tbase = windows[stage > 0 ? stage-1 : 0];

        if (tstamps[stage] == -1) {
            tstamps[stage] = tstamp;
            data[idx1] = nval;
            return;
        }

        if (tstamp-tstamps[stage] < tbase) {
            data[idx1] += nval;
            return;
        }

        if (stage < stages.length-1) {
            shift(tstamp+tbase, data[idx2], stage+1);
        }

        for (int i = idx2; i > idx1; i--) {
            data[i] = data[i-1];
        }

        data[idx1] = nval;
        tstamps[stage] += tbase;
    }


    public long getDeltaV(long tstamp, int stage) {

        if (stage < 0 || stage >= stages.length) {
            return 0L;
        }

        int len = stages[stage], idx1 = offsets[stage];

        long delta = 0L;

        for (int i = idx1; i < idx1+len; i++) {
            delta += data[i];
        }

        return delta;
    }


    public long getDeltaT(long tstamp, int stage) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
