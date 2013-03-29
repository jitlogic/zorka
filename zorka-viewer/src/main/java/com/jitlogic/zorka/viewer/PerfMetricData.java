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

package com.jitlogic.zorka.viewer;

import com.jitlogic.zorka.core.util.Metric;
import com.jitlogic.zorka.core.util.ZorkaUtil;

public class PerfMetricData {

    public static final int NEW_LEN = 512;

    private int scannerId;
    private Metric metric;

    private long[] tstamps;
    private long[] lvals;
    private double[] dvals;
    private int count;


    public PerfMetricData(Metric metric) {
        this.metric = metric;
        tstamps = new long[NEW_LEN];
    }


    public void addL(long clock, long value) {
        if (lvals == null) {
            lvals = new long[NEW_LEN];
        } else if (count == tstamps.length) {
            tstamps = ZorkaUtil.clipArray(tstamps, tstamps.length+NEW_LEN);
            lvals = ZorkaUtil.clipArray(lvals, lvals.length+NEW_LEN);
        }

        tstamps[count] = clock;
        lvals[count] = value;
        count++;
    }


    public void addD(long clock, double value) {
        if (dvals == null) {
            dvals = new double[NEW_LEN];
        } else if (count == tstamps.length) {
            tstamps = ZorkaUtil.clipArray(tstamps, tstamps.length+NEW_LEN);
            dvals = ZorkaUtil.clipArray(dvals, dvals.length+NEW_LEN);
        }
        tstamps[count] = clock;
        dvals[count] = value;
        count++;
    }


    public Metric getMetric() {
        return metric;
    }

    public long getT(int idx) {
        return idx < tstamps.length && idx >= 0 ? tstamps[idx] : 0;
    }

    public long getL(int idx) {
        return lvals != null && idx < lvals.length && idx >= 0 ? lvals[idx] : 0;
    }

    public double getD(int idx) {
        return dvals != null && idx < dvals.length && idx >= 0 ? dvals[idx] : 0;
    }

    public double getV(int idx) {
        if (dvals != null) {
            return idx < dvals.length && idx >= 0 ? dvals[idx] : 0;
        } else if (lvals != null) {
            return idx < lvals.length && idx >= 0 ? lvals[idx] : 0;
        } else {
            return 0.0;
        }
    }

    public boolean isL() {
        return lvals != null;
    }

    public boolean isD() {
        return dvals != null;
    }

    public int size() {
        return count;
    }

    public int getScannerId() {
        return scannerId;
    }

    public void setScannerId(int scannerId) {
        this.scannerId = scannerId;
    }
}
