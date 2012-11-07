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

import java.util.Arrays;

/**
 * Bucket aggregate is
 */
public class BucketAggregate {

    public static final long NS   = 1L;
    public static final long MS   = NS * 1000;
    public static final long SEC  = MS * 1000;
    public static final long MIN  = SEC * 60;
    public static final long HOUR = MIN * 60;
    public static final long DAY  = HOUR * 24;

    private long base, total, tstart;
    private int[] stages, offsets;

    private long[] data, windows, tstamps;


    public BucketAggregate(long base, int...stages) {
        this.tstart = -1;
        this.base = base;

        this.stages = new int[stages.length+1];

        this.stages[0] = 1;
        System.arraycopy(stages, 0, this.stages, 1, stages.length);

        this.total = 0;

        this.init();
    }


    private void init() {
        windows = new long[stages.length]; windows[0] = base;
        offsets = new int[stages.length]; offsets[0] = 0;
        tstamps = new long[stages.length]; tstamps[0] = 0;

        int dlen = 1;

        for (int i = 1; i < stages.length; i++) {
            windows[i] = windows[i-1] * stages[i];
            offsets[i] = offsets[i-1] + stages[i-1];
            tstamps[i] = 0;
            dlen += stages[i];
        }

        data = new long[dlen];
    }


    public int size() {
        return stages.length;
    }


    public long[] getWindows() {
        return Arrays.copyOf(windows, windows.length);
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


    public long getBase() {
        return base;
    }


    public long getTime() {
        return 0L;
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
    }


    private void shift(long tstamp, long nval, int stage) {
        int len = stages[stage], idx1 = offsets[stage], idx2 = idx1 + len - 1;
        long tbase = windows[stage > 0 ? stage-1 : 0];

        if (tstamp-tstamps[stage] <= tbase) {
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


    public double getAverage(int stage) {

        if (stage < 0 || stage >= stages.length) {
            return 0.0;
        }

        return (double)getDelta(stage)/(double)windows[stage];
    }


    public double getAverage(long window) {
        return getDelta(getStage(window));
    }


    public long getDelta(int stage) {

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


    public long getDelta(long window) {
        return getDelta(getStage(window));
    }


}
