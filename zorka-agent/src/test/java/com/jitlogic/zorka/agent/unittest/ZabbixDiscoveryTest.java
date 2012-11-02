package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.AgentGlobals;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.rankproc.AvgRateCounter;
import com.jitlogic.zorka.agent.testutil.TestExecutor;
import com.jitlogic.zorka.agent.testutil.TestJmx;
import com.jitlogic.zorka.agent.testutil.JmxTestUtil;
import com.jitlogic.zorka.zabbix.ZabbixLib;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ZabbixDiscoveryTest {

    private JmxTestUtil jmxTestUtil = new JmxTestUtil();
    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;
    private ZabbixLib zabbixLib;
    private AvgRateCounter counter;


    @Before
    public void setUp() {
        AgentGlobals.setMBeanServerRegistry(new MBeanServerRegistry(true));
        bshAgent = new ZorkaBshAgent(new TestExecutor());
        zorkaLib = bshAgent.getZorkaLib();
        zabbixLib = new ZabbixLib(bshAgent, zorkaLib);
        jmxTestUtil.setUp(bshAgent);
        counter = new AvgRateCounter(zorkaLib);
    }


    @After
    public void tearDown() {
        jmxTestUtil.tearDown();
        AgentGlobals.setMBeanServerRegistry(null);
    }


    @Test
    public void testSimpleDiscovery() {
        JSONObject obj = zabbixLib.discovery("java", "java.lang:type=GarbageCollector,*","name");
        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        assertTrue("Must return more than 1 item", data.size() > 1);
    }


    @Test
    public void testDiscoveryWithInternalAttrs() throws Exception {
        TestJmx jmx1 = jmxTestUtil.makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        jmx1.getStrMap().put("a", "aaa");

        TestJmx jmx2 = jmxTestUtil.makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
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
        TestJmx jmx1 = jmxTestUtil.makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        jmx1.getStrMap().put("a", "aaa");
        jmx1.getStrMap().put("c", "ccc");

        TestJmx jmx2 = jmxTestUtil.makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        jmx2.getStrMap().put("b", "bbb");

        JSONObject obj = zabbixLib.discovery("test", "test:type=TestJmx,*", new String[]{ "name" },
                new String[]{ "StrMap", "~.*" },
                new String[]{ null, "key" });

        assertTrue("Must return JSONObject", obj instanceof JSONObject);
        JSONArray data = (JSONArray)obj.get("data");
        //assertTrue("Must return more than 1 item", data.size() > 1);
        assertEquals(3, data.size());
    }
}
