/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.util.ZorkaUtil;

public class PerfSample {

    public static final byte LONG_SAMPLE = 1;

    public static final byte DOUBLE_SAMPLE = 2;

    private transient Object result;
    private transient Metric metric;

    private int metricId;

    /** Clock normally isn't used by agent nor encoded in trace files. */
    private transient long clock;


    private Number value;

    public PerfSample(int metricId, Number value) {
        this.metricId = metricId;
        this.value = value;
    }


    public int getMetricId() {
        return metricId;
    }


    public Number getValue() {
        return value;
    }


    public int getType() {
        return value instanceof Long ? LONG_SAMPLE : DOUBLE_SAMPLE;
    }


    @Override
    public String toString() {
        return "PerfSample(" + metric.getId() + ", " + value + ")";
    }


    @Override
    public int hashCode() {
        return 31 * metric.getId() + 17 * value.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PerfSample) {
            PerfSample sample = (PerfSample)obj;
            return ZorkaUtil.objEquals(metric, this.metric)
                && ZorkaUtil.objEquals(value, sample.value);
        } else {
            return false;
        }
    }

    public long getClock() {
        return clock;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }
}
