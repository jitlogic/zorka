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

package com.jitlogic.zorka.agent.testinteg;

import static org.junit.Assert.*;

import java.net.URL;


import com.jitlogic.zorka.agent.MBeanServerRegistry;
import org.junit.Before;
import org.junit.Test;

import com.jitlogic.zorka.agent.TimeoutThreadPoolExecutor;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaCallback;

public class BshAgentIntegTest {

	private ZorkaBshAgent agent;
	
	private volatile Object result = null;
	private volatile Throwable err = null;
	
	
	// Use it only with synchronous executor 
	private Object execute(final String expr, long sleep) throws Exception {
		
		agent.exec(expr, new ZorkaCallback() {
			
			public void handleResult(Object rslt) {
				result = rslt;
			}
			
			public void handleError(Throwable e) {
				err = e;
			}
		});
		
		long tstart = System.currentTimeMillis(); 
		while (result == null && err == null) {
			Thread.sleep(1);
			if (System.currentTimeMillis()-tstart > sleep) {
				fail("Timed out waiting for request completion.");
			}
		}
		
		return result;
	}

	@Before
	public void setUp() {
		agent = new ZorkaBshAgent(
            //new ClosingTimeoutExecutor(5, 5, 100));
			TimeoutThreadPoolExecutor.newBoundedPool(100), new MBeanServerRegistry());
		result = null;
		err = null;
	}
	
	@Test
	public void testAgentFunctions() throws Exception {
		assertEquals(ZorkaBshAgent.VERSION, execute("zorka.version()", 1000));
	}

	@Test
	public void testJmxCalls() throws Exception {
		Object obj = execute( "zorka.jmx(\"java\",\"java.lang:type=Runtime\",\"SpecVersion\")", 1000);
		//assertEquals("1.0", obj);
        assertTrue(obj instanceof String);
	}

	@Test
	public void testAgentTimeout() throws Exception {
		URL script = this.getClass().getResource("/integTest.bsh");
		agent.loadScript(script);
		//assertNull(null, execute("testLoopForever()", 1000));  // TODO zobaczyc co z tym jest grane 
		//assertTrue("should reach execution timeout", err instanceof ThreadDeath);
	}

}
