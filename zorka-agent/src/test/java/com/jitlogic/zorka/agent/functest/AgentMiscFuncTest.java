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

package com.jitlogic.zorka.agent.functest;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.ObjectDumper;
import com.jitlogic.zorka.agent.TimeoutThreadPoolExecutor;
import com.jitlogic.zorka.agent.ZorkaBasicCallback;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaBshWorker;

public class AgentMiscFuncTest {

	ZorkaBshAgent agent;
	
	private Object query(final String src) throws Exception {
		ZorkaBasicCallback callback = new ZorkaBasicCallback();
		ZorkaBshWorker worker = new ZorkaBshWorker(agent, src, callback);
		worker.run();
		return callback.getResult();
	}
	
	@Before
	public void setUp() {
		agent = new ZorkaBshAgent(TimeoutThreadPoolExecutor.newBoundedPool(5, 100, 10));
	}
	
	@After
	public void tearDown() {
		agent.svcStop();
		agent = null;
	}
	
	@Test
	public void testStartAndLoadScripts() throws Exception {
		agent.svcStart();
		URL url = AgentMiscFuncTest.class.getResource("/cfg1");
		agent.loadScriptDir(url);
		assertEquals("oja! right!", query("testLoadScriptDir()"));
	}
	
	@Test
	public void testObjectDumper() throws Exception {
		agent.svcStart();
		Object obj = query("zorka.jmx('java','java.lang:type=Runtime')");
		ObjectDumper dumper = new ObjectDumper();
		String s = dumper.dump(obj);
		System.out.println(s);
	}
	
	
}
