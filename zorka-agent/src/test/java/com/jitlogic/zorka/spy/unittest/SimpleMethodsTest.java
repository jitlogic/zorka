/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.spy.unittest;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.bootstrap.AgentMain;
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
		agent = new ZorkaBshAgent(new TestExecutor(), new MBeanServerRegistry(true));
		lib = new ZorkaSpyLib(agent);
		spy = lib.getSpy();
        agent.getMBeanServerRegistry().lookup("java");
        AgentMain.agent = new JavaAgent();
    }
	
	@After
	public void tearDown() {
		agent.svcStop();
		MainCollector.clear();
	}
	
	
	@Test
	public void testTrivialMethod() throws Exception {
		Object obj = makeCall("someMethod");		
		assertEquals(1, obj.getClass().getField("runCounter").get(obj));
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
	
	
	@Test
	public void testInstrumentTryCatchFinallyMethod() throws Exception {
		Object obj = makeCall("tryCatchFinallyMethod", "OKAU");
		assertEquals(1, obj.getClass().getField("runCounter").get(obj));
		assertEquals(1, obj.getClass().getField("finCounter").get(obj));
		checkStats("tryCatchFinallyMethod", 1, 0, 1);
	}


    @Test
    public void testInstrumentAndCheckRetVal() throws Exception {
        Object obj = makeCall("methodWithStringRetVal", 1);
        Object ret = TestUtil.callMethod(obj, "methodWithStringRetVal", 1);
        assertEquals("Returning: 1", ret);
    }

    @Test
    public void testInstrumentWithSubsequenctCall() throws Exception {
        Object obj = makeCall("testWithSubsequentCall", "aaa");
        Object ret = TestUtil.callMethod(obj, "testWithSubsequentCall", "aaa");
        assertEquals(true, ret);

    }

	
	private Object makeCall(String method, Object...args) throws Exception {
		
		lib.simple("com.jitlogic.zorka.spy.unittest.SomeClass", method, 
				"zorka:type=ZorkaStats,name=SomeClass", "stats");
		
		Object obj = TestUtil.instrumentAndInstantiate(spy, 
				"com.jitlogic.zorka.spy.unittest.SomeClass");

		TestUtil.callMethod(obj, method, args);
		
		return obj;
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
	}

}
