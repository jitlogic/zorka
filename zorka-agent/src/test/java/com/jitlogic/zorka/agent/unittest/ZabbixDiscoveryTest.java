package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.*;
import com.jitlogic.zorka.agent.testutil.*;
import com.jitlogic.zorka.rankproc.AvgRateCounter;
import com.jitlogic.zorka.api.ZabbixLib;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;

import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZabbixDiscoveryTest extends ZorkaFixture {

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
