package com.jitlogic.zorka.spy.unittest;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

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

public class SimpleMethodsTest {

	ZorkaBshAgent agent;
	ZorkaSpy spy;
	ZorkaSpyLib lib;
	
	@Before
	public void setUp() throws Exception {
		agent = new ZorkaBshAgent(new TestExecutor());
		lib = new ZorkaSpyLib(agent);
		spy = lib.getSpy();
	}
	
	@After
	public void tearDown() {
		agent.svcStop();
		MainCollector.clear();
	}
	
	
	@Test
	public void testTrivialMethod() throws Exception {
		lib.simple("com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod", 
					"zorka:type=ZorkaStats,name=SomeClass", "stats");
		
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
					"com.jitlogic.zorka.spy.unittest.SomeClass");
		
		assertNotNull(obj);
		
		TestUtil.callMethod(obj, "someMethod");
		
		checkStats("someMethod", 1, 0, 1);
	}
	
	
	@Test
	public void testInstrumentErrorMethod() throws Exception {

		lib.simple("com.jitlogic.zorka.spy.unittest.SomeClass", "errorMethod", 
				"zorka:type=ZorkaStats,name=SomeClass", "stats");
	
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
				"com.jitlogic.zorka.spy.unittest.SomeClass");
	
		assertNotNull(obj);

		try {
			TestUtil.callMethod(obj, "errorMethod");
			fail("errorMethod should throw TestException");
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof TestException)) {
				e.printStackTrace();
				fail("errorMethod should throw TestException but threw " + e.getCause().getClass() + " instead.");
			}
		}

		checkStats("errorMethod", 1, 1, 1);
	}

	
	@Test
	public void testInstrumentIndirectErrorMethod() throws Exception {
		
		lib.simple("com.jitlogic.zorka.spy.unittest.SomeClass", "indirectErrorMethod", 
				"zorka:type=ZorkaStats,name=SomeClass", "stats");
	
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
				"com.jitlogic.zorka.spy.unittest.SomeClass");
	
		assertNotNull(obj);
		
		try {
			TestUtil.callMethod(obj, "indirectErrorMethod");
			fail("errorMethod should throw TestException");
		} catch (InvocationTargetException e) {
			if (!(e.getCause() instanceof TestException)) {
				e.printStackTrace();
				fail("errorMethod should throw TestException but threw " + e.getCause().getClass() + " instead.");
			}
		}

		checkStats("indirectErrorMethod", 1, 1, 2);		
	}
	
	
	private void checkStats(String method, long calls, long errors, long time) throws Exception {
		
		MethodCallStats stats = (MethodCallStats)agent.getZorkaLib().jmx(
			"java", "zorka:type=ZorkaStats,name=SomeClass", "stats");
		
		assertNotNull(stats);
		
		MethodCallStatistic mcs = stats.getMethodCallStat(method);
		
		assertNotNull(mcs);
		assertEquals("number of calls", calls, mcs.getCalls());
		assertEquals("number of errors", errors, mcs.getErrors());
		assertEquals("execution time", time, mcs.getTotalTime());
	}
	
}
