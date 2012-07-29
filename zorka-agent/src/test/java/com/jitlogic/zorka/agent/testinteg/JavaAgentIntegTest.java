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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.ZorkaBshAgent;

public class JavaAgentIntegTest {

	private String query(String qry) throws Exception {
		Socket client = new Socket("127.0.0.1", 10055);
		
		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		out.writeBytes(qry+"\n");
		out.flush();
		
		DataInputStream in = new DataInputStream(client.getInputStream());
		byte[] buf = new byte[4096];
		int len = in.read(buf);
		
		byte[] sbuf = new byte[len-13];
		for (int i = 0; i < len-13; i++)
			sbuf[i] = buf[i+13];
		return new String(sbuf);
	}

	@Before
	public void setUp() throws Exception {
		Thread.sleep(10);
		String path = this.getClass().getResource("/conf").getPath();
		System.setProperty("zorka.home.dir", path);
		JavaAgent.start();
		Thread.sleep(10);
	}
	
	@After
	public void tearDown() throws Exception {
		JavaAgent.stop();
		Thread.sleep(10);		
	}
	
	@Test
	public void testStartAndStopTibcoAgent() throws Exception {
		assertEquals(ZorkaBshAgent.VERSION, query("zorka__version[]"));		
	}

	//@Test  TODO fix this
	public void testForCustomScriptsLoaded() throws Exception {
		assertEquals("oja!", query("tescik[]"));
	}
}
