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

import com.jitlogic.zorka.agent.*;
import com.jitlogic.zorka.agent.rankproc.AvgRateCounter;
import com.jitlogic.zorka.agent.integ.ZabbixLib;
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

    private ZorkaLib zorkaLib;
    private ZabbixLib zabbixLib;
    private AvgRateCounter counter;


    @Before
    public void setUp() {
        zorkaLib = zorkaAgent.getZorkaLib();
        zabbixLib = new ZabbixLib(zorkaAgent, zorkaLib);
        counter = new AvgRateCounter(zorkaLib);
    }


    @Test
    public void testSimpleDiscovery() throws Exception {
        TestJmx jmx1 = makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        TestJmx jmx2 = makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);

        JSONObject obj = zabbixLib.discovery("test", "test:type=TestJmx,*","name");
        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        assertTrue("Must return more than 1 item", data.size() > 1);
    }


    @Test
    public void testDiscoveryWithInternalAttrs() throws Exception {
        TestJmx jmx1 = makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        jmx1.getStrMap().put("a", "aaa");

        TestJmx jmx2 = makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        jmx2.getStrMap().put("b", "bbb");

        JSONObject obj = zabbixLib.discovery("test", "test:type=TestJmx,*", new String[]{ "name" },
                new String[]{ "StrMap", "~.*" },
                new String[]{ null, "key" });

        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        //assertTrue("Must return more than 1 item", data.size() > 1);
        assertEquals(2, data.size());
    }


    @Test
    public void testDiscoveryWithMoreThanOneInternapAttr() throws Exception {
        TestJmx jmx1 = makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        jmx1.getStrMap().put("a", "aaa");
        jmx1.getStrMap().put("c", "ccc");

        TestJmx jmx2 = makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        jmx2.getStrMap().put("b", "bbb");

        JSONObject obj = zabbixLib.discovery("test", "test:type=TestJmx,*", new String[]{ "name" },
                new String[]{ "StrMap", "~.*" },
                new String[]{ null, "key" });

        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        //assertTrue("Must return more than 1 item", data.size() > 1);
        assertEquals(3, data.size());
    }

    // TODO zabbix discovery algorithm is quite complicated and thus it must be tested thoroughly.


    private TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        testMbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

}
