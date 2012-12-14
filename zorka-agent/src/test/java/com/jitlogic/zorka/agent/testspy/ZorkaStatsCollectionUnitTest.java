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

package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.rankproc.BucketAggregate;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.spy.collectors.JmxAttrCollector;
import com.jitlogic.zorka.spy.collectors.ZorkaStatsCollector;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Date;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.getAttr;
import static com.jitlogic.zorka.spy.SpyLib.*;

public class ZorkaStatsCollectionUnitTest extends ZorkaFixture {

    @Test
    public void testCollectToStatsMbeanWithoutPlaceholders() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=Test", "stats", "test", 0, 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.process(SpyLib.ON_COLLECT, sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=Test", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("test"));
    }


    @Test
    public void testCollectorToStatsMbeanWithMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=Test", "stats", "${methodName}", 0, 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.process(SpyLib.ON_COLLECT, sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=Test", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectoToStatsMbeanWithClassAndMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=${shortClassName}", "stats", "${methodName}", 0, 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.process(SpyLib.ON_COLLECT, sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=TClass", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectToStatsWithKeyExpression() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=${shortClassName}", "stats", "${1}", 0, 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L, "oja" });
        collector.process(SpyLib.ON_COLLECT, sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=TClass", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("oja"));
    }

    // TODO test for actual calls/errors collection

    // TODO test if ctx<->stats pair is properly cached


    @Test
    public void testJmxAttrCollectorTrivialCall() throws Exception {
        JmxAttrCollector collector = new JmxAttrCollector("test", "test:name=${shortClassName}", "${methodName}", 0, 1);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        SpyRecord record = new SpyRecord(ctx);
        record.feed(ON_RETURN, new Object[]{});  // Mark proper return from method
        record.feed(ON_COLLECT, new Object[]{BucketAggregate.SEC, BucketAggregate.SEC/2 });
        collector.process(SpyLib.ON_COLLECT, record);

        assertEquals(1L, getAttr("test", "test:name=TClass", "testMethod_calls"));
        assertEquals(0L, getAttr("test", "test:name=TClass", "testMethod_errors"));
        assertEquals(500L, getAttr("test", "test:name=TClass", "testMethod_time"));

        Object date = getAttr("test", "test:name=TClass", "testMethod_last");
        assertTrue("Should return java.util.Date object", date instanceof Date);
        assertEquals(1000L, ((Date)date).getTime());
    }


    @Test
    public void testJmxAttrCollectorTrivialError() throws Exception {
        JmxAttrCollector collector = new JmxAttrCollector("test", "test:name=${shortClassName}", "${methodName}", 0, 1);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        SpyRecord record = new SpyRecord(ctx);
        record.feed(ON_ERROR, new Object[]{});  // Mark proper return from method
        record.feed(ON_COLLECT, new Object[]{BucketAggregate.SEC, BucketAggregate.SEC/2 });
        collector.process(SpyLib.ON_COLLECT, record);

        assertEquals(1L, getAttr("test", "test:name=TClass", "testMethod_calls"));
        assertEquals(1L, getAttr("test", "test:name=TClass", "testMethod_errors"));
        assertEquals(500L, getAttr("test", "test:name=TClass", "testMethod_time"));

        Object date = getAttr("test", "test:name=TClass", "testMethod_last");
        assertTrue("Should return java.util.Date object", date instanceof Date);
        assertEquals(1000L, ((Date)date).getTime());
    }
}
