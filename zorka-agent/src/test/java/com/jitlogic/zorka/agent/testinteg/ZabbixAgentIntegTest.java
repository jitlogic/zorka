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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.MBeanServerRegistry;
import com.jitlogic.zorka.agent.testutil.TestLogger;
import com.jitlogic.zorka.integ.ZorkaLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.integ.ZabbixAgent;
import com.jitlogic.zorka.integ.ZabbixRequestHandler;
import com.jitlogic.zorka.agent.ZorkaConfig;

public class ZabbixAgentIntegTest {

	private String query(String qry) throws Exception {
		Socket client = new Socket("127.0.0.1", 10066);
		
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
	
	private ZorkaBshAgent agent = null;
	private ZabbixAgent service = null;
	
	@Before
	public void setUp() throws Exception {
        ZorkaConfig.loadProperties(this.getClass().getResource("/conf").getPath());
        ZorkaLogger.setLogger(new TestLogger());
        AgentInstance.setMBeanServerRegistry(new MBeanServerRegistry(true));
		agent = new ZorkaBshAgent(Executors.newSingleThreadExecutor());
		ZorkaConfig.getProperties().put ("zabbix.listen.addr", "127.0.0.1");
		ZorkaConfig.getProperties().put("zabbix.listen.port", "10066");
		service = new ZabbixAgent(agent);
		service.start();
	}
	
	@After
	public void tearDown() {
		service.stop();
        AgentInstance.setMBeanServerRegistry(null);
        ZorkaLogger.setLogger(null);
        ZorkaConfig.cleanup();
	}
	
	@Test
	public void testTrivialRequestAsync() throws Exception {
		assertEquals(ZorkaConfig.getProperties().getProperty("zorka.version"),
                query("zorka__version[]"));
	}
	
	@Test
	public void testJavaQuery() throws Exception {
		String rslt = query("zorka__jmx[\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]");
		assertFalse("Query has crashed.", 
			ZabbixRequestHandler.ZBX_NOTSUPPORTED.equals(rslt));
	}

	
}
