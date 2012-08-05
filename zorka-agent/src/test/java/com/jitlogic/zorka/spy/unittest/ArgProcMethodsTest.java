package com.jitlogic.zorka.spy.unittest;

import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.MBeanServerRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.testutil.TestExecutor;
import com.jitlogic.zorka.mbeans.MethodCallStatistic;
import com.jitlogic.zorka.mbeans.MethodCallStats;
import com.jitlogic.zorka.spy.MainCollector;
import com.jitlogic.zorka.spy.ZorkaSpy;
import com.jitlogic.zorka.spy.ZorkaSpyLib;

public class ArgProcMethodsTest {

	ZorkaBshAgent agent;
	ZorkaSpy spy;
	ZorkaSpyLib lib;
	
	
	@Before
	public void setUp() throws Exception {
		agent = new ZorkaBshAgent(new TestExecutor(), new MBeanServerRegistry());
		lib = new ZorkaSpyLib(agent);
		spy = lib.getSpy();
	}
	
	
	@After
	public void tearDown() {
		agent.svcStop();
		MainCollector.clear();
		MethodCallStats stats = (MethodCallStats)agent.getZorkaLib().jmx(
				"java", "zorka:type=ZorkaStats,name=SomeClass", "stats");
		stats.clear();
	}
	
	
	@Test
	public void testOneArgMethod() throws Exception {
		makeAndCall("singleArgMethod", "{1}", "oja");
		checkStats("oja", 1, 0, 1);
		
	}
	
	
	@Test
	public void testOneArgMethodWithSomeFormatting() throws Exception {
		makeAndCall("singleArgMethod", "ozesz.{1}", "oja");
		checkStats("ozesz.oja", 1, 0, 1);		
	}
	
	
	@Test
	public void testOneArgMethodWithAttrHandling() throws Exception {
		makeAndCall("singleArgMethod", "{1.class.name}", "oja");
		checkStats("java.lang.String", 1, 0, 1);				
	}
	

	@Test
	public void testThreeArgMethod() throws Exception {
		makeAndCall("threeArgMethod", "{1}.{2}.{3}", "a", "b", "c");
		checkStats("a.b.c", 1, 0, 1);
	}
	
	private void makeAndCall(String method, String format, Object...args) throws Exception {
		lib.simple("com.jitlogic.zorka.spy.unittest.SomeClass", method, 
				"zorka:type=ZorkaStats,name=SomeClass", "stats", format);
	
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
				"com.jitlogic.zorka.spy.unittest.SomeClass");
	
		assertNotNull(obj);
	
		TestUtil.callMethod(obj, method, args);
	}
	
	private void checkStats(String method, long calls, long errors, long time) throws Exception {
		
		MethodCallStats stats = (MethodCallStats)agent.getZorkaLib().jmx(
			"java", "zorka:type=ZorkaStats,name=SomeClass", "stats");
		
		assertNotNull(stats);
		
		MethodCallStatistic mcs = stats.getMethodCallStat(method);
		
		assertNotNull(mcs);
		assertEquals("number of calls", calls, mcs.getCalls());
		assertEquals("number of errors", errors, mcs.getErrors());
		//assertEquals("execution time", time, mcs.getTotalTime());
		assertTrue("execution time >= 0", mcs.getTotalTime() > 0);
	}
}
