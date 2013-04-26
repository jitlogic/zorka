/** 
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.integ;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

import com.jitlogic.zorka.core.integ.ZabbixQueryTranslator;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.test.support.TestLogger;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.integ.ZabbixAgent;
import com.jitlogic.zorka.core.integ.ZabbixRequestHandler;
import com.jitlogic.zorka.core.ZorkaConfig;
import static org.fest.assertions.Assertions.assertThat;

public class ZabbixAgentIntegTest {

    private ZabbixQueryTranslator translator;

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
    private ZorkaConfig config;
	
	@Before
	public void setUp() throws Exception {
        config = new ZorkaConfig(this.getClass().getResource("/conf").getPath());
        ZorkaLogger.setLogger(new TestLogger());
        translator = new ZabbixQueryTranslator();
        agent = new ZorkaBshAgent(Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor(), 5000,
                new MBeanServerRegistry(true), config, translator);
		config.getProperties().put ("zabbix.listen.addr", "127.0.0.1");
		config.getProperties().put("zabbix.listen.port", "10066");
		service = new ZabbixAgent(config,  agent, translator);
		service.start();
	}
	
	@After
	public void tearDown() {
		service.stop();
	}
	
	@Test
	public void testTrivialRequestAsync() throws Exception {
		assertEquals(config.getProperties().getProperty("zorka.version"),
                query("zorka.version[]"));
	}
	
	@Test
	public void testJavaQuery() throws Exception {
		assertThat(query("zorka__jmx[\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]"))
                .isNotEqualTo(ZabbixRequestHandler.ZBX_NOTSUPPORTED);
	}

    @Test
	public void testNonAllowedFn() throws Exception {
        translator.allow("zorka.version");

        assertThat(query("zorka.version[]"))
                .isNotEqualTo(ZabbixRequestHandler.ZBX_NOTSUPPORTED);

        assertThat(query("zorka__jmx[\"java\", \"java.lang:type=OperatingSystem\", \"Arch\"]"))
            .isEqualTo(ZabbixRequestHandler.ZBX_NOTSUPPORTED);

        assertThat(query("zorka.version[]"))
            .isNotEqualTo(ZabbixRequestHandler.ZBX_NOTSUPPORTED);
    }
}
