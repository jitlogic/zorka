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

public class CircularBucketAggregate implements BucketAggregate {

    private long base, total, tstart, tlast;
    private int res;


    /**
     * Length of each section.  [stages.length]
     */
    private long[] windows;


    /**
     * Timestamps of the beggining of each window.   [stages.length]
     */
    private long[] ts;


    /**
     * Fist and last sample timestamp in each section. Initially tmin = tmax = -1.
     * For sections with  tmin points to the end of the section 1 and tmax points to the beginning.
     *
     */
    private long[] tmin, tmax;               // stages.length * res


    /**
     * Collected data.
     */
    private long[] values;               // stages.length * res


    public CircularBucketAggregate(long base, int res, int...stages) {
        this.base = base;
        this.res = res;

        this.windows = new long[stages.length];

        this.ts = new long[stages.length];
        this.tmin = new long[stages.length * res];
        this.tmax = new long[stages.length * res];
        this.values = new long[stages.length * res];

        for (int i = 0; i < windows.length; i++) {
            // TODO check if resolution fits
            windows[i] = base * stages[i];
            ts[i] = 0;
            cleanupStage(i);
        }
    }

    private void cleanupStage(int stage) {
        int offs = stage * this.res;
        for (int j = offs; j < offs + this.res; j++) {
            tmin[j] = tmax[j] = -1;
            values[j] = 0;
        }
    }


    public int size() {
        return windows.length;
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
        return tlast - tstart;
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


    private void feedWindow(int stage, long tstamp, long value) {
        int offset = stage * res;
        long window = windows[stage], sectn = window / res;

        if (tstamp - ts[stage] < 0) {
            return;
        } else if (tstamp - ts[stage] > window) {
            cleanupStage(stage);
            ts[stage] = tstamp - (tstamp % sectn);
            tmin[offset] = tmax[offset] = tstamp;
            values[offset] = value;
        } else if (tstamp - ts[stage] > window/res) {
            long tss = tstamp % sectn;
            while (tstamp > ts[stage] + sectn) {
                for (int i = res-1; i > 0; i--) {
                    tmin[offset+i] = tmin[offset+i-1];
                    tmax[offset+i] = tmax[offset+i-1];
                    values[offset+i] = values[offset+i-1];
                }
                ts[stage] += sectn;
                tmin[offset] = ts[stage];
                tmax[offset] = ts[stage] + res;
                values[offset] = 0;
            } // while ()
            values[offset] = value;
            tmin[offset] = tmax[offset] = tstamp;
        } else {
            values[offset] += value;
            tmin[offset] = tmin[offset] != -1 ? Math.min(tmin[offset], tstamp) : tstamp;
            tmax[offset] = tmax[offset] != -1 ? Math.max(tmax[offset], tstamp) : tstamp;
        }

    }


    public void feed(long tstamp, long value) {
        for (int i = 0; i < windows.length; i++) {
            feedWindow(i, tstamp, value);
        }

        total += value;

        if (tstart == -1) {
            tstart = tstamp;
        }

        tlast = Math.max(tlast, tstamp);
    }


    public long getDeltaV(long tstamp, int stage) {
        int offset = stage * res;
        long v = 0;

        for (int i = offset; i < offset + res; i++) {
            v += values[i];
        }

        return v;
    }


    public long getDeltaT(long tstamp, int stage) {
        int offset = stage * res;
        long t1 = -1, t2 = -1;

        for (int i = offset; i < offset + res; i++) {
            if (t1 == -1) {
                t1 = tmin[i];
            }

            if (tmax[i] != -1) {
                t2 = tmax[i];
            }
        }

        return Math.abs(t1-t2);
    }
}
