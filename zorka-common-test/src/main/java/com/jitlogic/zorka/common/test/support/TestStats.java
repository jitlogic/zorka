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

import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;

public class TestStats implements Stats {

    private Statistic[] stats;
    private String[] names;

    public TestStats() {
        stats = new Statistic[3];
        names = new String[3];

        stats[0] = new TestStat("aaa", 0, 0);
        names[0] = stats[0].getName();
        stats[1] = new TestStat("bbb", 1, 1);
        names[1] = stats[1].getName();
        stats[2] = new TestStat("ccc", 2, 2);
        names[2] = stats[2].getName();
    }

    public Statistic getStatistic(String statisticName) {
        for (int i = 0; i < names.length; i++) {
            if (statisticName.equals(names[i])) {
                return stats[i];
            }
        }

        return null;
    }

    public String[] getStatisticNames() {
        return ZorkaUtil.copyArray(names);
    }

    public Statistic[] getStatistics() {
        return stats;
    }
}
