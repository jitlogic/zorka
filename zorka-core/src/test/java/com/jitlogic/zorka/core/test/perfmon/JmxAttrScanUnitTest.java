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

package com.jitlogic.zorka.core.test.perfmon;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.test.support.TestJmx;
import com.jitlogic.zorka.common.tracedata.Metric;
import com.jitlogic.zorka.common.tracedata.PerfRecord;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.perfmon.TraceOutputJmxScanner;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

public class JmxAttrScanUnitTest extends ZorkaFixture {

    ZorkaSubmitter<SymbolicRecord> out;
    List<Object> results = new ArrayList<Object>();

    @Before
    public void createSomeMBeans() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "uja", "wuja");
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "eja", "weja");
        out = new ZorkaSubmitter<SymbolicRecord>() {
            @Override
            public boolean submit(SymbolicRecord record) {
                return results.add(record);
            }
        };
    }


    @Test
    public void testSimpleNullScan() throws Exception {
        TraceOutputJmxScanner scanner = perfmon.scanner("TEST",
                new QueryDef("test", "test:type=XXX,*", "name").get("Nom")
                        .metric(perfmon.metric("test", "test", "test")));
        ObjectInspector.setField(scanner, "output", out);
        scanner.runCycle(100);
        Assert.assertEquals(0, results.size());
    }


    @Test
    public void testSimpleScanWithOneResult() throws Exception {
        TraceOutputJmxScanner scanner = perfmon.scanner("TEST",
                new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom")
                        .metric(perfmon.metric("test", "test", "test")));
        ObjectInspector.setField(scanner, "output", out);
        scanner.runCycle(100);
        Assert.assertEquals(1, results.size());
    }


    @Test
    public void testCheckIfMetricObjectsAreRegisteredAndHaveIds() throws Exception {
        TraceOutputJmxScanner scanner = perfmon.scanner("TEST",
                new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom")
                        .metric(perfmon.metric("test", "test", "test")));
        ObjectInspector.setField(scanner, "output", out);
        scanner.runCycle(100);
        List<PerfSample> samples = ((PerfRecord) results.get(0)).getSamples();

        Assert.assertNotNull(samples);
        Assert.assertEquals(2, samples.size());

        for (PerfSample sample : samples) {
            Assert.assertTrue("Should have non-zero metric ID", sample.getMetricId() > 0);
            Metric metric = agentInstance.getMetricsRegistry().getMetric(sample.getMetricId());
            Assert.assertTrue("Template should have non-zero metric ID", metric.getTemplate().getId() > 0);
        }
    }


    // TODO check for dynamic attributes being passed and attached correctly

    @Test
    public void testCheckIfDynamicAttributesArePassedCorrectly() throws Exception {
        TraceOutputJmxScanner scanner = perfmon.scanner("TEST",
                new QueryDef("test", "test:type=TestJmx,*", "name").getAs("Nom", "ATTR")
                        .metric(
                                perfmon.metric("test", "test", "test").dynamicAttrs("ATTR")));
        ObjectInspector.setField(scanner, "output", out);

        scanner.runCycle(100);
        List<PerfSample> samples = ((PerfRecord) results.get(0)).getSamples();

        Assert.assertNotNull(samples);
        Assert.assertEquals(2, samples.size());

        for (PerfSample sample : samples) {
            Assert.assertNotNull(sample.getAttrs());
            Assert.assertEquals(1, sample.getAttrs().size());
        }
    }


    @Test
    public void testCheckIfOutputDataIsProperlyCast() throws Exception {
        TraceOutputJmxScanner scanner = perfmon.scanner("TEST",
                new QueryDef("test", "test:type=TestJmx,*", "name").getAs("Nom", "ATTR")
                        .metric(
                                perfmon.metric("test", "test", "test").dynamicAttrs("ATTR")));
        ObjectInspector.setField(scanner, "output", out);

        scanner.runCycle(100);
        List<PerfSample> samples = ((PerfRecord) results.get(0)).getSamples();

        Assert.assertNotNull(samples);
        Assert.assertEquals(2, samples.size());

        for (PerfSample sample : samples) {
            Assert.assertNotNull(sample.getAttrs());
            Assert.assertEquals(Long.class, sample.getValue().getClass());
        }
    }


    private TestJmx makeTestJmx(String name, long nom, long div, String... md) throws Exception {
        TestJmx bean = new TestJmx();

        bean.setNom(nom);
        bean.setDiv(div);

        for (int i = 1; i < md.length; i += 2) {
            bean.put(md[i - 1], md[i]);
        }

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
