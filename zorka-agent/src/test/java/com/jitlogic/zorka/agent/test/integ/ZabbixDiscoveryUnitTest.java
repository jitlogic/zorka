/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.test.integ;

import com.jitlogic.zorka.agent.integ.ZabbixLib;
import com.jitlogic.zorka.agent.mbeans.MethodCallStatistics;
import com.jitlogic.zorka.agent.mbeans.ZorkaMappedMBean;
import com.jitlogic.zorka.agent.perfmon.QueryDef;
import com.jitlogic.zorka.agent.test.support.TestJmx;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;

import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZabbixDiscoveryUnitTest extends ZorkaFixture {

    private ZabbixLib zabbixLib;


    @Before
    public void setUp() {
        zabbixLib = new ZabbixLib();
    }


    @Test
    public void testSimpleDiscovery() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        JSONObject obj = zabbixLib.discovery("test", "test:type=TestJmx,*","name", "type");
        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        assertTrue("Must return more than 1 item", data.size() > 1);
    }


    @Test
    public void testDiscoveryUsingQueryFramework() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        JSONObject obj = zabbixLib.discovery(zorka.query("test", "test:type=TestJmx,*", "name", "type"));
        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        assertTrue("Must return more than 1 item", data.size() > 1);
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
        JSONObject obj1 = zabbixLib.discovery(query1);
        assertEquals("query with exact attrs should return data", 2, ((JSONArray)obj1.get("data")).size());


        QueryDef query2 = zorka.query("test", "test:type=ZorkaStats").get("stats").listAs("**", "PAR");
        JSONObject obj2 = zabbixLib.discovery(query2);
        assertEquals("query with redundant attrs should return no data", 0, ((JSONArray)obj2.get("data")).size());
    }


    private TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
