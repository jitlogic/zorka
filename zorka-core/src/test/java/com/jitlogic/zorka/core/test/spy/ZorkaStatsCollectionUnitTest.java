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

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.plugins.ZorkaStatsCollector;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Map;

import static com.jitlogic.zorka.core.test.support.TestUtil.getAttr;
import static com.jitlogic.zorka.core.perfmon.BucketAggregate.MS;
import static com.jitlogic.zorka.core.spy.SpyLib.*;

public class ZorkaStatsCollectionUnitTest extends ZorkaFixture {

    @Test
    public void testCollectToStatsMbeanWithoutPlaceholders() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=Test", "stats",
                "test", "T", null, ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "TClass", "testMethod", "()V", 1);

        Map<String, Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 10L);

        collector.process(record);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=Test", "stats");

        assertNotNull(stats.getStatistic("test"));
    }


    @Test
    public void testCollectorToStatsMbeanWithMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=Test", "stats",
                "${methodName}", "T", null, ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "TClass", "testMethod", "()V", 1);

        Map<String, Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 10L);

        collector.process(record);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=Test", "stats");

        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectoToStatsMbeanWithClassAndMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=${shortClassName}",
                "stats", "${methodName}", "T", null, ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "some.TClass", "testMethod", "()V", 1);

        Map<String, Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 10L);

        collector.process(record);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=TClass", "stats");

        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectToStatsWithKeyExpression() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=${shortClassName}",
                "stats", "${C1}", "T", null, ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "some.TClass", "testMethod", "()V", 1);

        Map<String, Object> record = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 10L, "C1", "oja");

        collector.process(record);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=TClass", "stats");

        assertNotNull(stats.getStatistic("oja"));
    }


    @Test
    public void testTwoCollectorsReportingToIndependentItemsInSingleZorkaStatsObject() throws Exception {
        new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "T",
                null, ZorkaStatsCollector.ACTION_STATS);

        new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "BBB", "T",
                null, ZorkaStatsCollector.ACTION_STATS);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=SomeBean", "stats");
        assertEquals(2, stats.getStatisticNames().length);
    }


    @Test
    public void testTwoCollectorsReportingToSingleZorkaStat() throws Exception {
        ZorkaStatsCollector c1 = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "T",
                null, ZorkaStatsCollector.ACTION_STATS);

        ZorkaStatsCollector c2 = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean", "stats", "AAA", "T",
                null, ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "some.TClass", "testMethod", "()V", 1);
        Map<String, Object> rec = ZorkaUtil.map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 1L);

        c1.process(rec);
        c2.process(rec);

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=SomeBean", "stats");
        assertEquals(2L, ((MethodCallStatistic) stats.getStatistic("AAA")).getCalls());
    }


    @Test
    public void testThroughputCounting() throws Exception {
        ZorkaStatsCollector c1 = new ZorkaStatsCollector(mBeanServerRegistry, "test", "test:name=SomeBean",
                "stats", "AAA", "T", "LEN", ZorkaStatsCollector.ACTION_STATS);

        SpyContext ctx = new SpyContext(spy.instance("x"), "some.TClass", "testMethod", "()V", 1);
        c1.process(ZorkaUtil.<String, Object>map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 1L, "LEN", 100));
        c1.process(ZorkaUtil.<String, Object>map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 1L, "LEN", 300));
        c1.process(ZorkaUtil.<String, Object>map(".CTX", ctx, ".STAGE", ON_SUBMIT, ".STAGES", (1 << ON_RETURN), "T", 1L, "LEN", 200));

        MethodCallStatistics stats = (MethodCallStatistics) getAttr(testMbs, "test:name=SomeBean", "stats");
        assertEquals(600L, ((MethodCallStatistic) stats.getStatistic("AAA")).getThroughput());
        assertEquals(300L, ((MethodCallStatistic) stats.getStatistic("AAA")).getMaxThroughput());
    }


    @Test
    public void testMaxTimeCLR() throws Exception {
        MethodCallStatistic stat = new MethodCallStatistic("A");

        stat.logCall(10L * MS);
        stat.logCall(20L * MS);

        assertEquals(20L, stat.getMaxTime());
        assertEquals(20L, stat.getMaxTimeCLR());
        assertEquals(0L, stat.getMaxTime());

        stat.logError(11L * MS);

        assertEquals(11L, stat.getMaxTimeCLR());
        assertEquals(0L, stat.getMaxTimeCLR());
    }


    @Test
    public void testThreadCounter() throws Exception {
        MethodCallStatistic stat = new MethodCallStatistic("A");

        stat.markEnter();
        assertEquals(1L, stat.getMaxThreads());
        assertEquals(1L, stat.getCurThreads());

        stat.markExit();
        assertEquals(1L, stat.getMaxThreads());
        assertEquals(0L, stat.getCurThreads());
    }

}
