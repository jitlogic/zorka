package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.agent.rankproc.AvgRateCounter;
import com.jitlogic.zorka.agent.testutil.TestExecutor;
import com.jitlogic.zorka.agent.testutil.TestUtil;
import com.jitlogic.zorka.agent.zabbix.ZabbixLib;
import com.jitlogic.zorka.mbeans.TabularDataWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.*;

/**
 */
public class TabularWrapperTest {

    private TestUtil testUtil = new TestUtil();
    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;
    private AvgRateCounter counter;

    @Before
    public void setUp() {
        bshAgent = new ZorkaBshAgent(new TestExecutor(), new MBeanServerRegistry());
        ZabbixLib zl = new ZabbixLib(bshAgent, bshAgent.getZorkaLib());
        zorkaLib = bshAgent.getZorkaLib();
        testUtil.setUp(bshAgent);
        counter = new AvgRateCounter(zorkaLib);
    }

    @After
    public void tearDown() {
        testUtil.tearDown();
    }

    private Map map(Object...vals) {
        HashMap<String,Object> ret = new HashMap<String, Object>(vals.length);

        for (int i = 0; i < vals.length; i+=2)
            ret.put(vals[i].toString(), vals[i+1]);

        return ret;
    }

    @Test
    public void testWrapMapOfMapsAndPresentAsAnMBean() throws Exception {
        List<?> lst = Arrays.asList(
                map("name", "aaa", "type", "ttt", "val", 1),
                map("name", "bbb", "type", "ttt", "val", 2)
        );

        TabularDataWrapper<Map> tdw = new TabularDataWrapper<Map>(Map.class, zorkaLib, lst, "asd", "name",
                new String[] { "name", "type", "val" },
                new OpenType[] {SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER  } );

        zorkaLib.mbean("java", "zorka:type=Test,name=TabularWrapperTest", "test").put("table", tdw);

        while (true) Thread.sleep(1000);
    }

}
