package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.core.integ.InfluxTracerOutput;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import com.jitlogic.zorka.core.test.support.TestStringOutput;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class InfluxDbIntegTest extends ZorkaFixture {

    @Before
    public void configureInfluxMetrics() {
    }

    @Test
    public void testSingleCycleWithZorkaStats() {


        configProperties.setProperty("metrics", "yes");
        configProperties.setProperty("ifluxdb", "yes");
        configProperties.setProperty("metrics.attr.const.host", "myhost");
        configProperties.setProperty("metrics.attr.const.app", "myapp");
        configProperties.setProperty("metrics.filter.exclude.diag", "*:type=ZorkaStats,name=Diagnostic,*");

        zorka.registerMbs("java", testMbs);

        assertNull(zorkaAgent.get("metrics"));
        zorka.require("ldap.bsh");
        zorka.require("apache/catalina.bsh");
        zorka.require("metrics.bsh");
        assertNotNull(zorkaAgent.get("metrics"));

        Map<String,String> cattr = zorka.mapCfg("metrics.attr.set");
        PerfAttrFilter attrFilter = perfmon.attrFilter(cattr,
                zorka.listCfg("metrics.attr.include"),
                zorka.listCfg("metrics.attr.exclude"));
        PerfSampleFilter sampleFilter = perfmon.sampleFilter(
                zorka.mapCfg("metrics.filter.include"),
                zorka.mapCfg("metrics.filter.exclude"));
        TestStringOutput httpOutput = new TestStringOutput();
        InfluxTracerOutput output = perfmon.influxOutput(cattr, attrFilter, sampleFilter, httpOutput);
        tracer.output(output);

        taskScheduler.runCycle("*");

        System.out.println(httpOutput);

        assertEquals("Only two sets of metrics should be visible.",
                8, httpOutput.getResults().size());

    }

}
