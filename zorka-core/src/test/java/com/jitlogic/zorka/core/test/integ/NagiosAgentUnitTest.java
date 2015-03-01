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

import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.integ.NrpePacket;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

public class NagiosAgentUnitTest extends ZorkaFixture {


    @Test
    public void testReadAndDecodeNrpePacket() throws Exception {
        NrpePacket pkt = NrpePacket.fromStream(getClass().getResourceAsStream("/nagios/test_nrpe.bin"));

        assertEquals(2, pkt.getVersion());
        assertEquals(1, pkt.getType());
        assertEquals("zorka.version[]", pkt.getData());
    }


    @Test
    public void testEncodeAndDecodePacket() throws Exception{
        NrpePacket pkt = NrpePacket.newInstance((short)2, (short)1, (short)1, "oja!");
        ByteArrayOutputStream os = new ByteArrayOutputStream(1036);

        pkt.encode(os);
        byte[] buf = os.toByteArray();
        assertEquals(1036, buf.length);

        NrpePacket pkt2 = NrpePacket.fromStream(new ByteArrayInputStream(buf));

        assertEquals(pkt.getVersion(), pkt2.getVersion());
        assertEquals(pkt.getType(), pkt2.getType());
        assertEquals(pkt.getResultCode(), pkt2.getResultCode());
        assertEquals(pkt.getData(), pkt2.getData());
    }

    @Test
    public void testEncodeAndDecodeWithNegativeCrc() throws Exception {
        NrpePacket pkt = NrpePacket.newInstance(2, NrpePacket.QUERY_PACKET, 3, "zorka.version[]");

        ByteArrayOutputStream os = new ByteArrayOutputStream(1036);

        pkt.encode(os);
        byte[] buf = os.toByteArray();
        assertEquals(1036, buf.length);

        NrpePacket pkt2 = NrpePacket.fromStream(new ByteArrayInputStream(buf));

        assertEquals(pkt.getVersion(), pkt2.getVersion());
        assertEquals(pkt.getType(), pkt2.getType());
        assertEquals(pkt.getResultCode(), pkt2.getResultCode());
        assertEquals(pkt.getData(), pkt2.getData());
    }
}
