/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.test.integ;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.test.util.ZorkaFixture;
import com.jitlogic.zorka.integ.NagiosAgent;

import com.jitlogic.zorka.integ.NrpePacket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.Socket;

import static org.junit.Assert.*;

public class NrpeAgentIntegTest extends ZorkaFixture {

    private NagiosAgent nagiosAgent;

    @Before
    public void setUp() {
        nagiosAgent = new NagiosAgent(zorkaAgent);
        nagiosAgent.start();
    }

    @After
    public void tearDown() {
        nagiosAgent.stop();
    }

    @Test
    public void testTrivialRequest() throws Exception {
        assertEquals(ZorkaConfig.getProperties().getProperty("zorka.version"), query("zorka.version[]"));
    }


    private String query(String query) throws Exception{
        Socket client = new Socket("127.0.0.1", 5669);

        NrpePacket request = NrpePacket.newInstance(2, NrpePacket.QUERY_PACKET, 3, "zorka.version[]");
        request.encode(client.getOutputStream());
        client.getOutputStream().flush();

        NrpePacket response = NrpePacket.fromStream(client.getInputStream());

        return response.getData();
    }
}
