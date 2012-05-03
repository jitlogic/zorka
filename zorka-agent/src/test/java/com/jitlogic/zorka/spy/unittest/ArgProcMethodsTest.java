package com.jitlogic.zorka.spy.unittest;

import static org.junit.Assert.*;

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
		lib.byArgs("com.jitlogic.zorka.spy.unittest.SomeClass", "singleArgMethod", 
					"zorka:type=ZorkaStats,name=SomeClass", "stats", 1);
		
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
					"com.jitlogic.zorka.spy.unittest.SomeClass");
		
		assertNotNull(obj);
		
		TestUtil.callMethod(obj, "singleArgMethod", "oja");
		
		checkStats("oja", 1, 0, 1);
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
