/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import com.jitlogic.zorka.core.test.support.TestStringOutput;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class MetricsPushIntegTest extends ZorkaFixture {

    private Map<String,String> cattr;
    private PerfAttrFilter attrFilter;
    private PerfSampleFilter sampleFilter;
    private TestStringOutput httpOutput;


    @Before
    public void configureInfluxMetrics() {
        configProperties.setProperty("metrics.attr.const.host", "myhost");
        configProperties.setProperty("metrics.attr.const.app", "myapp");
        configProperties.setProperty("metrics.filter.exclude.diag", "*:type=ZorkaStats,name=Diagnostic,*");
        configProperties.setProperty("metrics.attr.exclude", "type");
        zorka.registerMbs("java", testMbs);

        cattr = zorka.mapCfg("metrics.attr.set");
        attrFilter = perfmon.attrFilter(cattr,
            zorka.listCfg("metrics.attr.include"),
            zorka.listCfg("metrics.attr.exclude"));
        sampleFilter = perfmon.sampleFilter(
            zorka.mapCfg("metrics.filter.include"),
            zorka.mapCfg("metrics.filter.exclude"));
        httpOutput = new TestStringOutput();
    }

    private void runScripts() {
        assertNull(zorkaAgent.get("metrics"));
        zorka.require("ldap.bsh");
        zorka.require("apache/catalina.bsh");
        zorka.require("metrics.bsh");
        //CommonTestUtil.printLogs(ZorkaLogLevel.TRACE);
        assertNotNull(zorkaAgent.get("metrics")); // Some function to check if scripts loaded properly
    }

    @Ignore("TBD fix metrics push") @Test
    public void testSingleInfluxCycleWithZorkaStats() {

        configProperties.setProperty("metrics", "yes");
        runScripts();
        tracer.output(perfmon.influxPushOutput(zorka.mapCfg("influxdb"), cattr, attrFilter, sampleFilter, httpOutput));
        taskScheduler.runCycle("*");

        assertEquals("Only two sets of metrics should be visible.",
                1, httpOutput.getResults().size());
        // TODO check results in more detail: syntax, filtering (no diagnostics), attrs (no type) etc.
    }

    @Ignore("TBD fix metrics push") @Test
    public void testSingleOpenTsdbCycleWithZorkaStats() {

        configProperties.setProperty("metrics", "yes");
        configProperties.setProperty("opentsdb.chunk.size", "256");
        runScripts();
        tracer.output(perfmon.tsdbPushOutput(zorka.mapCfg("opentsdb"),
                cattr, attrFilter, sampleFilter, httpOutput));
        taskScheduler.runCycle("*");

        assertTrue("Only two sets of metrics should be visible.", httpOutput.getResults().size() > 0);
        // TODO check results in more detail: json schema, filtering (no diagnostics), attrs (no type), etc.
    }

    @Ignore("TBD fix metrics push") @Test
    public void testSingleGraphiteCycleWithZorkaStats() {
        configProperties.setProperty("metrics", "yes");
        configProperties.setProperty("opentsdb.chunk.size", "256");
        runScripts();
        tracer.output(perfmon.graphitePushOutput(zorka.mapCfg("graphite"),
                cattr, attrFilter, sampleFilter, httpOutput));
        taskScheduler.runCycle("*");

        assertTrue("Only two sets of metrics should be visible.", httpOutput.getResults().size() > 0);
        // TODO check results in more detail: json schema, filtering (no diagnostics), attrs (no type), etc.
    }

    @Ignore("TBD fix metrics push") @Test
    public void testSinglePrometheusPushCycleWithZorkaStats() {
        configProperties.setProperty("metrics", "yes");
        runScripts();
        tracer.output(perfmon.prometheusPushOutput(zorka.mapCfg("prometheus.push"),
                cattr, attrFilter, sampleFilter, httpOutput));
        taskScheduler.runCycle("*");

        assertEquals("Only two sets of metrics should be visible.",
                1, httpOutput.getResults().size());
        // TODO check results in more detail: json schema, filtering (no diagnostics), attrs (no type), etc.

    }
}
