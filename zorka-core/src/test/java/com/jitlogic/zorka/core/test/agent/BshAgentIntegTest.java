/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.agent;

import static org.junit.Assert.*;

import java.net.URL;


import com.jitlogic.zorka.core.*;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Test;

public class BshAgentIntegTest extends ZorkaFixture {

	private volatile Object result = null;
	private volatile Throwable err = null;


    // Use it only with synchronous executor
	private Object execute(final String expr, long sleep) throws Exception {
		
		zorkaAgent.exec(expr, new ZorkaCallback() {
			
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

	@Test
	public void testAgentFunctions() throws Exception {
		assertEquals(
                configProperties.getProperty("zorka.version"),
                execute("zorka.version()", 1000));
        //AgentInstance.setMBeanServerRegistry(null);
	}

	@Test
	public void testJmxCalls() throws Exception {
        mBeanServerRegistry.register("java", java.lang.management.ManagementFactory.getPlatformMBeanServer(), null);
        Object obj = execute( "zorka.jmx(\"java\",\"java.lang:type=Runtime\",\"SpecVersion\")", 1000);
		//assertEquals("1.0", obj);
        assertTrue(obj instanceof String);
	}

	@Test
	public void testAgentTimeout() throws Exception {
		zorkaAgent.loadScript("/integTest.bsh");
		//assertNull(null, execute("testLoopForever()", 1000));  // TODO zobaczyc co z tym jest grane 
		//assertTrue("should reach execution timeout", err instanceof ThreadDeath);
	}

    @Test
    public void testLoadScriptFromClassPath() throws Exception {
        zorkaAgent.loadScript("test.bsh");
        assertEquals("yes", execute("testScriptIsLoaded()", 1000));
    }
}
