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
package com.jitlogic.zorka.core.perfmon;

/**
 * Default implementation of CircularBucketAggregate.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class CircularBucketAggregate implements BucketAggregate {

    /** Base unit */
    private long base;

    /** Sum of all submitted values */
    private long total;

    /** Timestamp of first and last value submitted */
    private long tfirst, tlast;

    /** Resolution of aggregate calculations */
    private int res;


    /** Length of each average (in base unit).  [averages.length] */
    private long[] averages;


    /** Timestamps of the beggining of each window.[averages.length] */
    private long[] ts;


    /**
     * Fist and last sample timestamp in each average. Initially tmin = tmax = -1.
     * For sections with  tmin points to the end of the section 1 and tmax points to the beginning.
     */
    private long[] tmin, tmax;  // averages.length * res


    /** Collected data (length=stages.length*res). */
    private long[] values;


    /**
     * Standard constructor.
     *
     * @param base base unit (eg. 1000000000 if time is passed in nanoseconds but averages are defined in seconds)
     *
     * @param res resolution of each average window
     *
     * @param averages lengths of calculated averages (defined in base units)
     */
    public CircularBucketAggregate(long base, int res, int...averages) {
        this.base = base;
        this.res = res;

        this.averages = new long[averages.length];

        this.ts = new long[averages.length];
        this.tmin = new long[averages.length * res];
        this.tmax = new long[averages.length * res];
        this.values = new long[averages.length * res];

        for (int i = 0; i < this.averages.length; i++) {
            // TODO check if resolution fits
            this.averages[i] = base * averages[i];
            ts[i] = 0;
            cleanup(i);
        }
    }


    /**
     * Cleans up given average aggregate
     *
     * @param averageIdx average index
     */
    private void cleanup(int averageIdx) {
        int offs = averageIdx * this.res;
        for (int j = offs; j < offs + this.res; j++) {
            tmin[j] = tmax[j] = -1;
            values[j] = 0;
        }
    }


    @Override
    public int size() {
        return averages.length;
    }


    @Override
    public long getWindow(int averageIdx) {
        return averages[averageIdx];
    }


    @Override
    public int getStage(long window) {
        for (int i = 0; i < averages.length; i++) {
            if (averages[i] == window) {
                return i;
            }

            if (averages[i] > window) {
                return (i > 0 && window- averages[i-1] < averages[i]-window) ? i-1 : i;
            }
        }

        return window < averages[averages.length-1] * 2 ? averages.length-1 : -1;
    }


    @Override
    public long getTime() {
        return tlast - tfirst;
    }


    @Override
    public long getLast() {
        return tlast;
    }


    @Override
    public long getTotal() {
        return total;
    }


    @Override
    public long getStart() {
        return tfirst;
    }


    /**
     * Submits value to a specific average
     *
     * @param averageIdx average index
     *
     * @param tstamp timestamp of submitted value
     *
     * @param value submitted value
     */
    private void feedAverage(int averageIdx, long tstamp, long value) {
        int offset = averageIdx * res;
        long window = averages[averageIdx], sectn = window / res;

        // TODO get rid of code below, use touch() method instead

        if (tstamp - ts[averageIdx] < 0) {
            return;
        } else if (tstamp - ts[averageIdx] > window) {
            cleanup(averageIdx);
            ts[averageIdx] = tstamp - (tstamp % sectn);
            tmin[offset] = tmax[offset] = tstamp;
            values[offset] = value;
        } else if (tstamp - ts[averageIdx] > window/res) {
            while (tstamp > ts[averageIdx] + sectn) {
                for (int i = res-1; i > 0; i--) {
                    tmin[offset+i] = tmin[offset+i-1];
                    tmax[offset+i] = tmax[offset+i-1];
                    values[offset+i] = values[offset+i-1];
                }
                ts[averageIdx] += sectn;
                tmin[offset] = ts[averageIdx];
                tmax[offset] = ts[averageIdx] + res;
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


    @Override
    public void feed(long tstamp, long value) {
        for (int i = 0; i < averages.length; i++) {
            feedAverage(i, tstamp, value);
        }

        total += value;

        if (tfirst == -1) {
            tfirst = tstamp;
        }

        tlast = Math.max(tlast, tstamp);
    }


    /**
     * Ensures that given average is in sync with timestamp. This is used to ensure that
     * readings of averages is accurate even if there is no
     *
     * @param averageIdx average index
     *
     * @param tstamp timestamp
     *
     */
    private void touch(int averageIdx, long tstamp) {
        int offset = averageIdx * res;
        long window = averages[averageIdx], sectn = window / res;

        if (tstamp - ts[averageIdx] < 0) {
            return;
        } else if (tstamp - ts[averageIdx] > window) {
            cleanup(averageIdx);
            ts[averageIdx] = tstamp - (tstamp % sectn);
            values[offset] = 0;
        } else if (tstamp - ts[averageIdx] > window/res) {
            while (tstamp > ts[averageIdx] + sectn) {
                for (int i = res-1; i > 0; i--) {
                    tmin[offset+i] = tmin[offset+i-1];
                    tmax[offset+i] = tmax[offset+i-1];
                    values[offset+i] = values[offset+i-1];
                }
                ts[averageIdx] += sectn;
                tmin[offset] = ts[averageIdx];
                tmax[offset] = ts[averageIdx] + res;
                values[offset] = 0;
            } // while ()
            values[offset] = 0;
            tmin[offset] = tmax[offset] = -1;
        }
    }


    @Override
    public long getDeltaV(int averageIdx, long tstamp) {
        int offset = averageIdx * res;
        long v = 0;

        touch(averageIdx, tstamp);

        for (int i = offset; i < offset + res; i++) {
            v += values[i];
        }

        return v;
    }


    @Override
    public long getDeltaT(int averageIdx, long tstamp) {
        int offset = averageIdx * res;
        long t1 = -1, t2 = -1;

        touch(averageIdx, tstamp);

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
