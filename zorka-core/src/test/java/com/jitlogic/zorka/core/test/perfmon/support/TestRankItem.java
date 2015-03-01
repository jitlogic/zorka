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
package com.jitlogic.zorka.core.test.perfmon.support;

import com.jitlogic.zorka.core.perfmon.Rankable;
import com.jitlogic.zorka.common.util.ZorkaUtil;

public class TestRankItem implements Rankable<TestRankItem> {

    private String[] metrics, averages;
    private double[] values;

    public TestRankItem(String[] metrics, String[] averages, double[] buf, int offset) {
        this.metrics = metrics;
        this.averages = averages;
        int len = metrics.length * averages.length;
        values = new double[len];
        System.arraycopy(buf, offset, values, 0, len);
    }

    public double getAverage(long tstamp, int metric, int average) {
        return values[metric*averages.length + average];
    }

    public String[] getMetrics() {
        return ZorkaUtil.copyArray(metrics);
    }

    public String[] getAverages() {
        return ZorkaUtil.copyArray(averages);
    }

    public TestRankItem getWrapped() {
        return this;
    }

    public String getName() {
        return "TestRankItem(" + values[0] + ")";
    }
}
