/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.integ;

import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.test.support.TestJmx;
import com.jitlogic.zorka.core.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Test;

import javax.management.ObjectName;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZabbixDiscoveryUnitTest extends ZorkaFixture {

    @Test
    public void testSimpleDiscovery() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        Map<String, List<Map<String, String>>> obj = zabbixLib._discovery("test", "test:type=TestJmx,*", "name", "type");

        assertTrue("Must return object", obj != null);
        assertTrue("Must return more than 1 item", obj.get("data").size() > 1);
    }


    @Test
    public void testSimpleDiscoveryWithIncorrectAttributeNamesThatShouldNotCauseNPE() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        Map<String, List<Map<String, String>>> obj = zabbixLib._discovery("test", "test:type=TestJmx,*", "bad", "type");

        assertTrue("Must return object", obj != null);
        assertTrue("Must return more than 1 item", obj.get("data").size() == 0);
    }


    @Test
    public void testSimpleDiscoveryAsJsonString() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        assertThat(zabbixLib.discovery("test", "test:type=TestJmx,*", "name", "type"))
                .contains("{#NAME}").contains("{#TYPE}").contains("TestJmx");
    }


    @Test
    public void testDiscoveryUsingQueryFramework() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        Map<String, List<Map<String, String>>> obj = zabbixLib._discovery(zorka.query("test", "test:type=TestJmx,*", "name", "type"));

        assertTrue("Must return object", obj != null);
        assertTrue("Must return more than 1 item", obj.get("data").size() > 1);
    }


    @Test
    public void testDiscoveryZorkaStats() throws Exception {
        ZorkaMappedMBean mbean = new ZorkaMappedMBean("test");
        MethodCallStatistics stats = new MethodCallStatistics();
        mbean.put("stats", stats);

        testMbs.registerMBean(mbean, new ObjectName("test:type=ZorkaStats"));

        stats.getMethodCallStatistic("A").logCall(4L);
        stats.getMethodCallStatistic("B").logCall(1L);

        QueryDef query1 = zorka.query("test", "test:type=ZorkaStats", "type").get("stats").listAs("**", "PAR");
        Map<String, List<Map<String, String>>> obj1 = zabbixLib._discovery(query1);
        assertEquals("query with exact attrs should return data", 2, obj1.get("data").size());


        QueryDef query2 = zorka.query("test", "test:type=ZorkaStats").get("stats").listAs("**", "PAR");
        Map<String, List<Map<String, String>>> obj2 = zabbixLib._discovery(query2);
        assertEquals("query with redundant attrs should return no data", 0, obj2.get("data").size());
    }

}
