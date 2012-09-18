package com.jitlogic.zorka.spy.unittest;

import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.testutil.TestExecutor;
import com.jitlogic.zorka.bootstrap.AgentMain;
import com.jitlogic.zorka.mbeans.MethodCallStats;
import com.jitlogic.zorka.spy.MainCollector;
import com.jitlogic.zorka.spy.ZorkaSpy;
import com.jitlogic.zorka.spy.ZorkaSpyLib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import static org.junit.Assert.assertNotNull;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class PresentingCollectorUnitTest {

    JavaAgent javaAgent;
    ZorkaBshAgent agent;
    ZorkaSpy spy;
    ZorkaSpyLib lib;

    MBeanServerRegistry mbsRegistry = new MBeanServerRegistry(true);


    @Before
    public void setUp() throws Exception {

        agent = new ZorkaBshAgent(new TestExecutor(), mbsRegistry);
        lib = new ZorkaSpyLib(agent);
        spy = lib.getSpy();

        javaAgent = new JavaAgent(new TestExecutor(), mbsRegistry, agent, lib);
        AgentMain.agent = javaAgent;

        mbsRegistry.lookup("java");
    }


    @After
    public void tearDown() {
        agent.svcStop();
        MainCollector.clear();
    }


    @Test
    public void testPresentStaticMethodAsValGetter() throws Exception {
        lib.present("com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod",
                "zorka:type=ZorkaStats1,name=SomeClass", "count",
                "com.jitlogic.zorka.spy.unittest.SomeClass",
                new String[] { },   // argPath
                new String[] { "count()" },
                ZorkaSpyLib.PRESENT_STATIC, true);

        Object obj = TestUtil.instrumentAndInstantiate(spy,
                "com.jitlogic.zorka.spy.unittest.SomeClass");

        assertNotNull(obj);
        TestUtil.callMethod(obj, "someMethod");

        assertEquals(123, agent.getZorkaLib().jmx("java", "zorka:type=ZorkaStats1,name=SomeClass", "count"));
    }


    @Test
    public void testPresentInstanceMethodAsValGetter() throws Exception {
        lib.present("com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod",
                "zorka:type=ZorkaStats2,name=SomeClass", "testcount",
                0,
                new String[] { },
                new String[] { "getTstCount()" },
                ZorkaSpyLib.PRESENT_ARGUMENT, true);

        Object obj = TestUtil.instrumentAndInstantiate(spy,
                "com.jitlogic.zorka.spy.unittest.SomeClass");

        assertNotNull(obj);
        TestUtil.callMethod(obj, "someMethod");
        TestUtil.callMethod(obj, "someMethod");
        TestUtil.callMethod(obj, "someMethod");

        assertEquals(126, agent.getZorkaLib().jmx("java", "zorka:type=ZorkaStats2,name=SomeClass", "testcount"));
    }


    @Test
    public void testPresentAttributeAsValGetter() throws Exception {
        lib.present("com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod",
                "zorka:type=ZorkaStats3,name=SomeClass", "runcounter",
                0,
                new String[] { },
                new String[] { ".runCounter" },
                ZorkaSpyLib.PRESENT_ATTRIBUTE, true);

        Object obj = TestUtil.instrumentAndInstantiate(spy,
                "com.jitlogic.zorka.spy.unittest.SomeClass");

        assertNotNull(obj);
        TestUtil.callMethod(obj, "someMethod");
        TestUtil.callMethod(obj, "someMethod");
        TestUtil.callMethod(obj, "someMethod");

        assertEquals(3, agent.getZorkaLib().jmx("java", "zorka:type=ZorkaStats3,name=SomeClass", "runcounter"));

    }
}
