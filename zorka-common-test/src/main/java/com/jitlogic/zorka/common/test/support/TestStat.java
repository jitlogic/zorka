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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.common.test.support;


import javax.management.j2ee.statistics.TimeStatistic;

public class TestStat implements TimeStatistic {

    private String name;
    private long count, time;

    public TestStat(String name, long count, long time) {
        this.name = name;
        this.count = count;
        this.time = time;
    }

    public long getCount() {
        return count;
    }

    public long getMaxTime() {
        return time;
    }

    public long getMinTime() {
        return time;
    }

    public long getTotalTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return "MS";
    }

    public String getDescription() {
        return name;
    }

    public long getStartTime() {
        return time;
    }

    public long getLastSampleTime() {
        return time;
    }
}
