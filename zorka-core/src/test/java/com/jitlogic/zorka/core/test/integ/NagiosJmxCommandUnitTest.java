/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import static org.junit.Assert.*;
import org.junit.Test;

public class NagiosJmxCommandUnitTest extends ZorkaFixture {

    @Test
    public void testTrivialJmxScan() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        NagiosJmxCommand cmd = (NagiosJmxCommand)
            nagiosLib.jmxscan(
              zorka.query("test", "test:type=TestJmx,*", "name").get("Nom")
                .metric(perfmon.metric("TEST", "TEST", "B"))
        ).withSummaryLine("TEST ${STATUS} - test item ${ATTR.name} ${LVAL0} ${UNIT0}; ")
         .withFirstResult().withLabel("${name}")
         .withPerfLine("${LABEL}=${LVAL0}${UNIT0};${LVAL0}");

        NrpePacket pkt = cmd.cmd();

        assertNotNull("should return some packet", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals("TEST OK - test item bean1 10 B; | bean1=10B;10", pkt.getData());
    }


    @Test
    public void testJmxScanForStatuses() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        NagiosJmxCommand cmd = (NagiosJmxCommand)
                nagiosLib.jmxscan(
                        zorka.query("test", "test:type=TestJmx,*", "name").get("Nom")
                                .metric(perfmon.metric("TEST", "TEST", "B"))
                ).withSummaryLine("TEST ${STATUS} - test item ${ATTR.name} ${LVAL0} ${UNIT0};")
                        .withFirstResult().withLabel("${name}")
                        .withPerfLine("${LABEL}=${LVAL0}${UNIT0};${LVAL0}");

        cmd.withRcMin("LVAL0", 15, 5);
        assertEquals("Should respond with WARN status",
                NrpePacket.WARN, cmd.cmd().getResultCode());
        cmd.withRcMin("LVAL0", 20, 15);

        assertEquals("Should respond with ERROR status",
                NrpePacket.ERROR, cmd.cmd().getResultCode());

        cmd.withRcMax("LVAL0", 7, 15);
        assertEquals("Should respond with WARN status",
                NrpePacket.WARN, cmd.cmd().getResultCode());

        cmd.withRcMax("LVAL0", 5, 9);
        assertEquals("Should respond with WARN status",
                NrpePacket.ERROR, cmd.cmd().getResultCode());
    }


    @Test
    public void testSumJmxScan() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        NagiosJmxCommand cmd = (NagiosJmxCommand)
                nagiosLib.jmxscan(
                    zorka.query("test", "test:type=TestJmx,*", "name").get("Nom")
                         .metric(perfmon.metric("TEST", "TEST", "B"))
                ).withSummaryLine("TEST ${STATUS} - test items summary ${LVAL0} ${UNIT0}; ")
                 .withLabel("${name}").withSumResult("sum")
                 .withTextLine("${ATTR.name} ${LVAL0} ${UNIT0}; ")
                 .withPerfLine("${LABEL}=${LVAL0}${UNIT0};${LVAL0}");

        NrpePacket pkt = cmd.cmd();

        assertNotNull("should return some packet", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals("TEST OK - test items summary 20 B; | sum=20B;20\n"
                   + "bean1 10 B; \n"
                   + "bean2 10 B; | bean1=10B;10\n"
                   + "bean2=10B;10"
                ,

                pkt.getData());

    }

    @Test
    public void testFilteredJmxScan() throws Exception {
        makeTestJmx("test:name=bean1,type=TestJmx", 10, 10);
        makeTestJmx("test:name=bean2,type=TestJmx", 10, 10);
        NagiosJmxCommand cmd = (NagiosJmxCommand)
                nagiosLib.jmxscan(
                        zorka.query("test", "test:type=TestJmx,*", "name").get("Nom")
                                .metric(perfmon.metric("TEST", "TEST", "B"))
                ).withSummaryLine("TEST ${STATUS} - test item ${ATTR.name} ${LVAL0} ${UNIT0}; ")
                        .withLabel("${name}").withFilterResult("name", "bean2")
                        .withTextLine("${ATTR.name} ${LVAL0} ${UNIT0}; ")
                        .withPerfLine("${LABEL}=${LVAL0}${UNIT0};${LVAL0}");

        NrpePacket pkt = cmd.cmd();

        assertNotNull("should return some packet", pkt);
        assertEquals(NrpePacket.OK, pkt.getResultCode());
        assertEquals("TEST OK - test item bean2 10 B; | bean2=10B;10\n"
                        + "bean1 10 B; | bean1=10B;10",
                pkt.getData());
    }

}
