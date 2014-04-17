/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.test.integ;


import com.jitlogic.zorka.core.integ.NagiosJmxCommand;
import com.jitlogic.zorka.core.integ.NrpePacket;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

public class NagiosJmxCmdUnitTest extends ZorkaFixture {

    @Test
    public void testTrivialSingleItemQuery() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        QueryDef query = zorka.query("test", "test:*", "name", "type");
        NagiosJmxCommand cmd = nagiosLib.jmxcmd(query, "TEST", "test item")
                .withPerfData("MB", "Nom", "Div").withAttrs("name", "Nom", "Div");
        NrpePacket pkt = cmd.cmd();
        assertNotNull("Should return some result", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals(
          "TEST OK - test item sum 10 MB (100%); | sum=10MB;10\n" +
          "bean1 10 MB (100%); | bean1=10MB;10", pkt.getData());
    }

    @Test
    public void testTrivialSingleItemQueryWithFirstItemSelected() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        QueryDef query = zorka.query("test", "test:*", "name", "type");
        NagiosJmxCommand cmd = nagiosLib.jmxcmd(query, "TEST", "test item")
                .withPerfData("MB", "Nom", "Div").withAttrs("name", "Nom", "Div")
                .withSelFirst();
        NrpePacket pkt = cmd.cmd();
        assertNotNull("Should return some result", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals("TEST OK - test item bean1 10 MB (100%); | bean1=10MB;10", pkt.getData());
    }

    @Test
    public void testTrivialTwoItemQuery() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        QueryDef query = zorka.query("test", "test:*", "name", "type");
        NagiosJmxCommand cmd = nagiosLib.jmxcmd(query, "TEST", "test item")
                .withPerfData("MB", "Nom", "Div").withAttrs("name", "Nom", "Div");
        NrpePacket pkt = cmd.cmd();
        assertNotNull("Should return some result", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals("TEST OK - test item sum 20 MB (100%); | sum=20MB;20\n" +
                "bean1 10 MB (100%); \n" +
                "bean2 10 MB (100%); | bean1=10MB;10\n" +
                "bean2=10MB;10"
                , pkt.getData());
    }


}
