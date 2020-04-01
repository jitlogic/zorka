/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.perfmon.*;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Before;
import org.junit.Test;

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
                scanner.getMetric(perfmon.metric("", "test", "test", "test", "gauge"), qr) instanceof Metric);

    }


    @Test
    public void testConstructSameAndUniqueMetrics() {
        MetricTemplate mt = perfmon.metric("","test", "test", "test", "gauge");

        Metric m1 = scanner.getMetric(mt, qr(10L, "name", "SomeObject", "type", "SomeType"));
        Metric m2 = scanner.getMetric(mt, qr(20L, "name", "SomeObject", "type", "SomeType"));
        Metric m3 = scanner.getMetric(mt, qr(10L, "name", "OtherObject", "type", "SomeType"));

        assertEquals("Should return the same metric", m1, m2);
        assertNotEquals("Should create new metric. ", m1, m3);
    }


    @Test
    public void testConstructMetricAndCheckForProperStringTemplating() {
        MetricTemplate mt = perfmon.metric("","test", "Very important ${name} metric.", "MT/s", "gauge");

        Metric m = scanner.getMetric(mt, qr(10L, "name", "SomeObject"));

        assertEquals("Very important SomeObject metric.", m.getDescription());
    }


    @Test
    public void testRawMetricWithoutMultiplierRetVal() {
        Metric m = metric(perfmon.metric("","", "", "", "gauge"));

        assertEquals(10L, m.getValue(0L, 10L));
    }


    @Test
    public void testRawMetricsWithRoundedMultiplier() {
        Metric m = metric(perfmon.metric("","", "", "", "gauge").multiply(2));

        assertEquals(20L, m.getValue(0L, 10L));
    }


    @Test
    public void testRawMetricWithUnRoundedMultiplier() {
        Metric m = metric(perfmon.metric("","", "", "", "gauge").multiply(1.23));

        assertEquals(12.3, (Double)m.getValue(0L, 10L), 0.001);
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
