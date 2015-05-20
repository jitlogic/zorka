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

import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.perfmon.*;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class MetricsFrameworkUnitTest extends ZorkaFixture {

    private JmxScanner scanner;

    @Before
    public void setScanner() {
        scanner = perfmon.scanner("test");
    }


    @Test
    public void testConstructMetricsOfVariousTypes() {
        QueryResult qr = qr(10L, "name", "SomeObject", "type", "SomeType");

        assertTrue("Should return RawDataMetric object.",
                scanner.getMetric(perfmon.metric("test", "test", "test"), qr) instanceof RawDataMetric);

        assertTrue("Should return RawDeltaMetric object.",
                scanner.getMetric(perfmon.delta("test", "test", "test"), qr) instanceof RawDeltaMetric);

        assertTrue("Expected TimerDeltaMetric object.",
                scanner.getMetric(perfmon.timedDelta("test", "test", "test"), qr) instanceof TimedDeltaMetric);

        assertTrue("Expected WindowedRateMetric",
                scanner.getMetric(perfmon.rate("t", "t", "t", "nom", "nom"), qr) instanceof WindowedRateMetric);
    }


    @Test
    public void testConstructSameAndUniqueMetrics() {
        MetricTemplate mt = perfmon.metric("test", "test", "test");

        Metric m1 = scanner.getMetric(mt, qr(10L, "name", "SomeObject", "type", "SomeType"));
        Metric m2 = scanner.getMetric(mt, qr(20L, "name", "SomeObject", "type", "SomeType"));
        Metric m3 = scanner.getMetric(mt, qr(10L, "name", "OtherObject", "type", "SomeType"));

        assertEquals("Should return the same metric", m1, m2);
        assertNotEquals("Should create new metric. ", m1, m3);
    }


    @Test
    public void testConstructSameAndUniqueMetricsWithDynamicAttr() {
        MetricTemplate mt = perfmon.metric("test", "test", "test").dynamicAttrs("name");

        Metric m1 = scanner.getMetric(mt, qr(10L, "name", "SomeObject", "type", "SomeType"));
        Metric m2 = scanner.getMetric(mt, qr(20L, "name", "OtherObject", "type", "SomeType"));
        Metric m3 = scanner.getMetric(mt, qr(30L, "name", "SomeObject", "type", "OtherType"));

        assertEquals("Should return the same metric regarless of 'name' attribute.", m1, m2);
        assertNotEquals("Should create new metric", m1, m3);
    }


    @Test
    public void testConstructMetricAndCheckForProperStringTemplating() {
        MetricTemplate mt = perfmon.metric("test", "Very important ${name} metric.", "MT/s");

        Metric m = scanner.getMetric(mt, qr(10L, "name", "SomeObject"));

        assertEquals("Very important SomeObject metric.", m.getDescription());
    }


    @Test
    public void testRawMetricWithoutMultiplierRetVal() {
        Metric m = metric(perfmon.metric("", "", ""));

        assertEquals(10L, m.getValue(0L, 10L));
    }


    @Test
    public void testRawMetricsWithRoundedMultiplier() {
        Metric m = metric(perfmon.metric("", "", "").multiply(2));

        assertEquals(20L, m.getValue(0L, 10L));
    }


    @Test
    public void testRawMetricWithUnRoundedMultiplier() {
        Metric m = metric(perfmon.metric("", "", "").multiply(1.23));

        assertEquals(12.3, (Double)m.getValue(0L, 10L), 0.001);
    }


    @Test
    public void testDeltaMetric() {
        Metric m = metric(perfmon.delta("", "", ""));

        assertEquals(0L, m.getValue(0L, 10L));
        assertEquals(5L, m.getValue(10L, 15L));
    }


    @Test
    public void testDeltaMetricWithRoundedMultiplier() {
        Metric m = metric(perfmon.delta("", "", "").multiply(2));

        assertEquals(0L, m.getValue(0L, 10L));
        assertEquals(10L, m.getValue(10L, 15L));
    }


    @Test
    public void testDeltaMetricWithUnRoundedMultiplier() {
        Metric m = metric(perfmon.delta("", "", "").multiply(2.5));

        assertEquals(0.0, (Double)m.getValue(0L, 10L), 0.001);
        assertEquals(12.5, (Double)m.getValue(10L, 15L), 0.001);
    }


    @Test
    public void testTimedDeltaMetric() {
        Metric m = metric(perfmon.timedDelta("", "", ""));

        assertEquals(0.0, (Double)m.getValue(0L, 10L), 0.001);
        assertEquals(2.0, (Double)m.getValue(5000L, 20L), 0.001);
    }


    @Test
    public void testTimedDeltaMetricWithMultiplier() {
        Metric m = metric(perfmon.timedDelta("", "", "").multiply(1.5));

        assertEquals(0.0, (Double)m.getValue(0L, 10L), 0.001);
        assertEquals(3.0, (Double)m.getValue(5000L, 20L), 0.001);
    }


    @Test
    public void testRateMetric() {
        Metric m = metric(perfmon.rate("", "", "", "a", "b"));

        Map<String,Long> v1 = ZorkaUtil.map("a", 0L, "b", 0L);
        Map<String,Long> v2 = ZorkaUtil.map("a", 10L, "b", 2L);

        assertEquals(0.0, (Double)m.getValue(0L, v1), 0.001);
        assertEquals(5.0, (Double)m.getValue(1L, v2), 0.001);
    }


    @Test
    public void testRateMetricWithMultiplier() {
        Metric m = metric(perfmon.rate("", "", "", "a", "b").multiply(0.2));

        Map<String,Long> v1 = ZorkaUtil.map("a", 0L, "b", 0L);
        Map<String,Long> v2 = ZorkaUtil.map("a", 10L, "b", 2L);

        assertEquals(0.0, (Double)m.getValue(0L, v1), 0.001);
        assertEquals(1.0, (Double)m.getValue(1L, v2), 0.001);

    }


    @Test
    public void testRateMetricWithNullValues() {
        Metric m = metric(perfmon.rate("", "", "", "a", "c"));

        Map<String,Long> v1 = ZorkaUtil.map("a", 0L, "b", 0L);
        Map<String,Long> v2 = ZorkaUtil.map("a", 10L, "b", 2L);

        assertEquals(0.0, (Double)m.getValue(0L, v1), 0.001);
        assertEquals(0.0, (Double)m.getValue(1L, v2), 0.001);
    }

    @Test
    public void testUtilizationMetric() {
        Metric m = metric(perfmon.util("", "", "", "a", "b"));

        Map<String,Long> v1 = ZorkaUtil.map("a", 5L, "b", 10L);
        Map<String,Long> v2 = ZorkaUtil.map("a", 1L, "b", 1L);

        assertEquals(50.0, (Double)m.getValue(0, v1), 0.001);
        assertEquals(100.0, (Double)m.getValue(0, v2), 0.001);
    }


    private QueryResult qr(Object val, Object...attrs) {
        QueryResult result = new QueryResult(val);
        for (int i = 1; i < attrs.length; i += 2) {
            result.setAttr(attrs[i-1].toString(), attrs[i]);
        }
        return result;
    }

    private Metric metric(MetricTemplate template) {
        return scanner.getMetric(template, qr(10L, "a", "a"));
    }


}
