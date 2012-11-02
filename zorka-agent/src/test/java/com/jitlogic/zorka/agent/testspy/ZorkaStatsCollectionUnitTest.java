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

import com.jitlogic.zorka.agent.AgentGlobals;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.spy.collectors.ZorkaStatsCollector;

import static com.jitlogic.zorka.spy.SpyConst.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.getAttr;

public class ZorkaStatsCollectionUnitTest {

    private MBeanServerRegistry registry;
    private MBeanServer testMbs;


    @Before
    public void setUp() {
        registry = new MBeanServerRegistry(true);
        testMbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        registry.register("test", testMbs, null);
        AgentGlobals.setMBeanServerRegistry(registry);
    }


    @After
    public void tearDown() {
        AgentGlobals.setMBeanServerRegistry(null);
    }


    @Test
    public void testCollectToStatsMbeanWithoutPlaceholders() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=Test", "stats", "test", 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.collect(sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=Test", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("test"));
    }


    @Test
    public void testCollectorToStatsMbeanWithMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=Test", "stats", "${methodName}", 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.collect(sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=Test", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("testMethod"));
    }


    @Test
    public void testCollectoToStatsMbeanWithClassAndMethodNamePlaceholder() throws Exception {
        ZorkaStatsCollector collector = new ZorkaStatsCollector("test", "test:name=${shortClassName}", "stats", "${methodName}", 0);
        SpyContext ctx = new SpyContext(new SpyDefinition(), "some.TClass", "testMethod", "()V", 1);

        SpyRecord sr = new SpyRecord(ctx);
        sr.feed(ON_COLLECT, new Object[] { 10L });
        collector.collect(sr);

        MethodCallStatistics stats =  (MethodCallStatistics)getAttr("test", "test:name=TClass", "stats");

        assertNotNull("", stats);
        assertNotNull(stats.getStatistic("testMethod"));
    }

    // TODO test if ctx<->stats pair is properly cached
}
