package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaLib;
import com.jitlogic.zorka.agent.rankproc.AvgRateCounter;
import com.jitlogic.zorka.agent.testutil.TestJmx;
import com.jitlogic.zorka.agent.testutil.TestUtil;
import com.jitlogic.zorka.agent.zabbix.ZabbixLib;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AverageRateCountingTest {

    private static class TrivialExecutor implements Executor {
        public void execute(Runnable command) {
            command.run();
        }
    }

    private TestUtil testUtil = new TestUtil();
    private ZorkaBshAgent bshAgent;
    private ZorkaLib zorkaLib;
    private AvgRateCounter counter;

    @Before
    public void setUp() {
        bshAgent = new ZorkaBshAgent(new TrivialExecutor(), new MBeanServerRegistry());
        ZabbixLib zl = new ZabbixLib(bshAgent, bshAgent.getZorkaLib());
        zorkaLib = bshAgent.getZorkaLib();
        testUtil.setUp(bshAgent);
        counter = zorkaLib.newRateCounter();
    }

    @After
    public void tearDown() {
        testUtil.tearDown();
    }

    @Test
    public void testRegisterAndQueryTrivialBean() throws Exception {
        testUtil.makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        assertEquals("10", bshAgent.query("zorka.jmx(\"test\", \"test:name=bean1,type=TestJmx\", \"Nom\")"));
    }

    @Test
    public void testOneObjectAvgRateCount() throws Exception {
        TestJmx tj = testUtil.makeTestJmx("test:name=bean1,type=TestJmx", 0, 0);
        List<Object> path = counter.list("test", "test:name=bean1,type=TestJmx");

        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
        tj.setNom(5); tj.setDiv(5);

        assertEquals(1.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.5, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);
    }

    @Test
    public void testOneObjectTwoAverages() throws Exception {
        TestJmx tj = testUtil.makeTestJmx("test:name=bean1,type=TestJmx", 0, 0);
        List<Object> path = counter.list("test", "test:name=bean1,type=TestJmx");

        tj.setNom(5); tj.setDiv(5);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG5), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.33, counter.get(path, "Nom", "Div", AvgRateCounter.AVG1), 0.01);

        tj.setNom(10); tj.setDiv(20);
        assertEquals(0.0, counter.get(path, "Nom", "Div", AvgRateCounter.AVG5), 0.01);

    }
}
