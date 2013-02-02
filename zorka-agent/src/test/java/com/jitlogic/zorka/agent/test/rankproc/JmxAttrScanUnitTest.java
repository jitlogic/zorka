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

package com.jitlogic.zorka.agent.test.rankproc;

import com.jitlogic.zorka.agent.rankproc.JmxAttrScanner;
import com.jitlogic.zorka.agent.rankproc.QueryDef;
import com.jitlogic.zorka.agent.test.spy.support.TestTracer;
import com.jitlogic.zorka.agent.test.support.TestJmx;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;
import com.jitlogic.zorka.common.PerfSample;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.List;

public class JmxAttrScanUnitTest extends ZorkaFixture {

    @Before
    public void createSomeMBeans() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "uja", "wuja");
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10, "oja", "woja", "aja", "waja", "eja", "weja");
    }


    @Test
    public void testSimpleNullScan() {
        TestTracer output = new TestTracer();
        JmxAttrScanner scanner = tracer.jmxScanner("TEST", output,
                new QueryDef("test", "test:type=XXX,*", "name").get("Nom")
                        .withMetricTemplate(rankproc.rawDataMetric("test", "test")));
        scanner.runCycle(100);
        Assert.assertEquals(0, output.size());
    }


    @Test
    public void testSimpleScanWithOneResult() {
        TestTracer output = new TestTracer();
        JmxAttrScanner scanner = tracer.jmxScanner("TEST", output,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom")
                        .withMetricTemplate(rankproc.rawDataMetric("test", "test")));
        scanner.runCycle(100);
        Assert.assertEquals(1, output.size());
    }


    @Test
    public void testCheckIfMetricObjectsAreRegisteredAndHaveIds() {
        TestTracer output = new TestTracer();
        JmxAttrScanner scanner = tracer.jmxScanner("TEST", output,
                new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom")
                        .withMetricTemplate(rankproc.rawDataMetric("test", "test")));
        scanner.runCycle(100);
        List<PerfSample> samples = output.get(0, "samples");

        Assert.assertNotNull(samples);
        Assert.assertEquals(2, samples.size());

        for (PerfSample sample : samples) {
            Assert.assertTrue("Should have non-zero metric ID", sample.getMetricId() > 0);
        }
    }


    // TODO check for dynamic attributes being passed and attached correctly

    @Test
    public void testCheckIfDynamicAttributesArePassedCorrectly() {
        TestTracer output = new TestTracer();
        JmxAttrScanner scanner = tracer.jmxScanner("TEST", output,
            new QueryDef("test", "test:type=TestJmx,*", "name").get("Nom", "ATTR")
                .withMetricTemplate(
                    rankproc.rawDataMetric("test", "test").withDynamicAttr("ATTR")));

        scanner.runCycle(100);
        List<PerfSample> samples = output.get(0, "samples");

        Assert.assertNotNull(samples);
        Assert.assertEquals(2, samples.size());

        for (PerfSample sample : samples) {
            Assert.assertNotNull(sample.getAttrs());
            Assert.assertEquals(1, sample.getAttrs().size());
        }
    }


    private TestJmx makeTestJmx(String name, long nom, long div, String...md) throws Exception {
        TestJmx bean = new TestJmx();

        bean.setNom(nom); bean.setDiv(div);

        for (int i = 1; i < md.length; i += 2) {
            bean.put(md[i-1], md[i]);
        }

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
