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

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.spy.ZorkaStatsCollector;

import com.jitlogic.zorka.core.util.ZorkaUtil;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Map;

import static com.jitlogic.zorka.core.test.support.TestUtil.getAttr;
import static com.jitlogic.zorka.core.perfmon.BucketAggregate.MS;
import static com.jitlogic.zorka.core.spy.SpyLib.*;

public class ZorkaStatsCollectionUnitTest extends ZorkaFixture {

    @Test
    public void testCollectToStatsMbeanWithoutPlaceholders() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=Test", "stats", "test", "C0");
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        Map<String,Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "S0", 10L);

        collector.process(record);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr(testMbs, "test:name=Test", "stats");

        assertNotNull(stats.getStatistic("test"));
    }


    @Test
    public void testCollectorToStatsMbeanWithMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=Test", "stats", "${methodName}", "S0");
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        Map<String,Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1<<ON_RETURN), "S0", 10L);

        collector.process(record);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr(testMbs, "test:name=Test", "stats");

        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectoToStatsMbeanWithClassAndMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=${shortClassName}", "stats", "${methodName}", "S0");
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        Map<String,Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1<<ON_RETURN), "S0", 10L);

        collector.process(record);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr(testMbs, "test:name=TClass", "stats");

        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectToStatsWithKeyExpression() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=${shortClassName}", "stats", "${C1}", "C0");
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        Map<String,Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1<<ON_RETURN), "C0", 10L, "C1", "oja");

        collector.process(record);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr(testMbs, "test:name=TClass", "stats");

        assertNotNull(stats.getStatistic("oja"));
    }


    @Test
    public void testTwoCollectorsReportingToIndependentItemsInSingleZorkaStatsObject() throws Exception {
        new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "C0");
        new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "BBB", "C0");

        MethodCallStatistics stats = (MethodCallStatistics)getAttr(testMbs, "test:name=SomeBean", "stats");
        assertEquals(2, stats.getStatisticNames().length);
    }

    @Test
    public void testTwoCollectorsReportingToSingleZorkaStat() throws Exception {
        ZorkaStatsCollector c1 = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "C0");
        ZorkaStatsCollector c2 = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "C0");

        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);
        Map<String,Object> rec = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1<<ON_RETURN), "C0", 1L);

        c1.process(rec);
        c2.process(rec);

        MethodCallStatistics stats = (MethodCallStatistics)getAttr(testMbs, "test:name=SomeBean", "stats");
        assertEquals(2L, ((MethodCallStatistic)stats.getStatistic("AAA")).getCalls());
    }

    @Test
    public void testMaxTimeCLR() throws Exception {
        MethodCallStatistic stat = new MethodCallStatistic("A");

        stat.logCall(10L*MS);
        stat.logCall(20L*MS);

        assertEquals(20L, stat.getMaxTime());
        assertEquals(20L, stat.getMaxTimeCLR());
        assertEquals(0L, stat.getMaxTime());

        stat.logError(11L*MS);

        assertEquals(11L, stat.getMaxTimeCLR());
        assertEquals(0L, stat.getMaxTimeCLR());
    }
}
