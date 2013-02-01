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

import com.jitlogic.zorka.agent.rankproc.*;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetricsFrameworkUnitTest extends ZorkaFixture {

    private QueryResult qr(Object val, Object...attrs) {
        QueryResult result = new QueryResult(val);
        for (int i = 1; i < attrs.length; i += 2) {
            result.setAttr(attrs[i-1].toString(), attrs[i]);
        }
        return result;
    }


    @Test
    public void testConstructMetricsOfVariousTypes() throws Exception {
        QueryResult qr = qr(10L, "name", "SomeObject", "type", "SomeType");

        assertTrue("Should return RawDataMetric object.",
                rankproc.rawDataMetric("test", "test").getMetric(qr) instanceof RawDataMetric);

        assertTrue("Should return RawDeltaMetric object.",
                rankproc.rawDeltaMetric("test", "test").getMetric(qr) instanceof RawDeltaMetric);

        assertTrue("Expected TimerDeltaMetric object.",
                rankproc.timedDeltaMetric("test", "test").getMetric(qr) instanceof TimedDeltaMetric);

        assertTrue("Expected WindowedRateMetric",
                rankproc.windowedRateMetric("t", "t", "nom", "nom").getMetric(qr) instanceof WindowedRateMetric);
    }


    @Test
    public void testConstructSameAndUniqueMetrics() throws Exception {
        MetricTemplate mt = rankproc.rawDataMetric("test", "test");

        Metric m1 = mt.getMetric(qr(10L, "name", "SomeObject", "type", "SomeType"));
        Metric m2 = mt.getMetric(qr(20L, "name", "SomeObject", "type", "SomeType"));
        Metric m3 = mt.getMetric(qr(10L, "name", "OtherObject", "type", "SomeType"));

        assertEquals("Should return the same metric", m1, m2);
        assertNotEquals("Should create new metric. ", m1, m3);
    }


    @Test
    public void testConstructSameAndUniqueMetricsWithDynamicAttr() throws Exception {
        MetricTemplate mt = rankproc.rawDataMetric("test", "test").withDynamicAttr("name");

        Metric m1 = mt.getMetric(qr(10L, "name", "SomeObject", "type", "SomeType"));
        Metric m2 = mt.getMetric(qr(20L, "name", "OtherObject", "type", "SomeType"));
        Metric m3 = mt.getMetric(qr(30L, "name", "SomeObject", "type", "OtherType"));

        assertEquals("Should return the same metric regarless of 'name' attribute.", m1, m2);
        assertNotEquals("Should create new metric", m1, m3);
    }


    @Test
    public void testConstructMetricAndCheckForProperStringTemplating() throws Exception {
        MetricTemplate mt = rankproc.rawDataMetric("Very important ${name} metric.", "MT/s");

        Metric m = mt.getMetric(qr(10L, "name", "SomeObject"));

        assertEquals("Very important SomeObject metric.", m.getName());
    }


    @Test
    public void testRawDataMetricWithoutMultiplierRetVal() throws Exception {
        QueryResult qr = qr(10L, "a", 1, "b", 2);

        MetricTemplate mt = rankproc.rawDataMetric("test", "req");

        assertEquals(10L, mt.getMetric(qr).getValue(0L, qr));
    }


    @Test
    public void testRawDataMetricsWithRoundedMultiplier() throws Exception {
        QueryResult qr = qr(10L, "a", 1);

        MetricTemplate mt = rankproc.rawDataMetric("test", "req").withMultiplier(2);

        assertEquals(20L, mt.getMetric(qr).getValue(0L, qr));
    }


    @Test
    public void testRawDataMetricWithUnroundedMultiplier() throws Exception {
        QueryResult qr = qr(10L, "a", 1);

        MetricTemplate mt = rankproc.rawDataMetric("test", "req").withMultiplier(1.23);

        assertEquals(12.3, (Double)mt.getMetric(qr).getValue(0L, qr), 0.001);
    }

    // TODO test raw delta cases

    // TODO test timed delta cases

    // TODO test windowed delta cases
}
