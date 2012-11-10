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
package com.jitlogic.zorka.agent.testrank;

import static com.jitlogic.zorka.rankproc.BucketAggregate.MS;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;

import com.jitlogic.zorka.rankproc.JmxAggregatingLister;
import com.jitlogic.zorka.rankproc.RankList;
import com.jitlogic.zorka.rankproc.RankLister;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RankZorkaStatsUnitTest extends ZorkaFixture {

    @Test
    public void testRankZorkaStatsByCallCount() {
        // TODO refactor this litany ...
        MethodCallStatistics stats = new MethodCallStatistics();

        MethodCallStatistic stat1 = stats.getMethodCallStatistic("stat1");
        MethodCallStatistic stat2 = stats.getMethodCallStatistic("stat2");
        MethodCallStatistic stat3 = stats.getMethodCallStatistic("stat3");

        stat1.logCall(400*MS, 200);
        stat2.logCall(100*MS, 100); stat2.logCall(200*MS, 100); stat2.logCall(300*MS, 100);
        stat3.logCall(150*MS, 200); stat3.logCall(250*MS, 100);

        RankList<MethodCallStatistic> rank = new RankList<MethodCallStatistic>(stats, 3, 0, 0, 10000*MS);

        List<MethodCallStatistic> lst = rank.list();
        assertEquals(stat2, lst.get(0));
        assertEquals(stat3, lst.get(1));
        assertEquals(stat1, lst.get(2));
    }


    @Test
    public void testListWithJmxAggregatingLister() throws Exception {
        // TODO refactor this litany ...
        MethodCallStatistics stats1 = new MethodCallStatistics();
        stats1.getMethodCallStatistic("aaa1"); stats1.getMethodCallStatistic("aaa2");
        MethodCallStatistics stats2 = new MethodCallStatistics();
        stats2.getMethodCallStatistic("bbb1"); stats2.getMethodCallStatistic("bbb2");
        MethodCallStatistics stats3 = new MethodCallStatistics();
        stats3.getMethodCallStatistic("ccc1"); stats3.getMethodCallStatistic("ccc2");

        mBeanServerRegistry.getOrRegisterBeanAttr("test", "zorka:type=TestStats,name=aaa", "stats1", stats1);
        mBeanServerRegistry.getOrRegisterBeanAttr("test", "zorka:type=TestStats,name=bbb", "stats2", stats2);
        mBeanServerRegistry.getOrRegisterBeanAttr("test", "zorka:type=TestStats,name=ccc", "stats3", stats3);

        RankLister<MethodCallStatistic> lister = new JmxAggregatingLister<MethodCallStatistic>("test", "zorka:type=TestStats,*");
        List<MethodCallStatistic> lst = lister.list();

        assertEquals(6, lst.size());
    }

    // TODO more tests for both bare ZorkaStats ranking and JMX aggregating lister
}
