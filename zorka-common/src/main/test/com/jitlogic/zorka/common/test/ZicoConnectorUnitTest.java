/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.test;


import com.jitlogic.zorka.common.test.support.TestZicoProcessor;
import com.jitlogic.zorka.common.test.support.TestZicoProcessorFactory;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.zico.AbstractZicoConnector;
import com.jitlogic.zorka.common.zico.ZicoClientConnector;
import com.jitlogic.zorka.common.zico.ZicoService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

public class ZicoConnectorUnitTest { //extends CentralFixture {

    ZicoService service;
    TestZicoProcessorFactory factory;

    @Before
    public void setUp() {
        factory = new TestZicoProcessorFactory();
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    private void waitUntilConn(int n) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (service.connCnt() < n) {
                Thread.sleep(1);
            } else {
                return;
            }
        }
    }


    private int signatureTest(int...inputs) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (int i : inputs) {
            bos.write(i);
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        AbstractZicoConnector.seekSignature(bis, AbstractZicoConnector.ZICO_MAGIC);
        return bis.read();
    }


    @Test(timeout = 100)
    public void testSearchZicoSignature1() throws Exception {
        assertEquals("No data after signature.", -1,
                signatureTest(AbstractZicoConnector.ZICO_MAGIC));

        assertEquals("Some data after signature.", 0x11,
                signatureTest(0x21, 0xC0, 0xBA, 0xBE, 0x11 ));

        assertEquals("Some garbage before signature.", 0x11,
                signatureTest(0x10, 0x21, 0xC0, 0xBA, 0xBE, 0x11));

        assertEquals("Partial and then complete signature.", 0x11,
                signatureTest(0x21, 0xC0, 0xBA, 0x21, 0xC0, 0xBA, 0xBE, 0x11));
    }


    @Test(timeout = 1000)
    public void testBasicConnectDisconnect() throws Exception {
        service = new ZicoService("127.0.0.1", 8640, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8640);
        conn.connect();
        conn.close();

        waitUntilConn(1);

        assertThat(service.connCnt()).describedAs("One connection attempt should be noticed.").isEqualTo(1);
    }


    @Test(timeout = 1000)
    public void testPingPong() throws Exception {
        service = new ZicoService("127.0.0.1", 8641, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8641);
        conn.connect();

        assertThat(conn.ping()).isGreaterThan(0);

        conn.close();
    }


    @Test(timeout = 1000)
    public void testHelloMessage() throws Exception {
        service = new ZicoService("127.0.0.1", 8642, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8642);
        conn.connect();

        short rslt = conn.sendData(new HelloRequest(10L, "test", "aaa"));
        assertEquals(AbstractZicoConnector.ZICO_OK, rslt);
        TestZicoProcessor proc = factory.getPmap().get("test");
        assertNotNull("New data processor should be registered.", proc);
        assertEquals("Should have no records received.", 0, proc.getResults().size());

        conn.close();
    }


    @Test(timeout = 1000)
    public void testSendSimpleSymbolMessage() throws Exception {
        service = new ZicoService("127.0.0.1", 8643, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8643);
        conn.connect();

        assertEquals(AbstractZicoConnector.ZICO_OK, conn.sendData(new HelloRequest(10L, "test", "aaa")));
        assertEquals(AbstractZicoConnector.ZICO_OK, conn.sendData(new Symbol(1, "test")));

        TestZicoProcessor proc = factory.getPmap().get("test");
        assertNotNull("New data processor should be registered.", proc);
        assertEquals("Should have no records received.", 1, proc.getResults().size());
        assertEquals(new Symbol(1, "test"), proc.getResults().get(0));

        conn.close();
    }


    @Test(timeout = 1000)
    public void sendUnauthorizedMsg() throws Exception {
        service = new ZicoService("127.0.0.1", 8644, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8644);
        conn.connect();

        assertEquals("Should return ZICO_AUTH_ERROR.",
                AbstractZicoConnector.ZICO_AUTH_ERROR, conn.sendData(new Symbol(1, "test")));

        conn.close();
    }


    @Test(timeout = 1000)
    public void sendBadLoginMsg() throws Exception {
        service = new ZicoService("127.0.0.1", 8644, factory);
        service.start();

        ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8644);
        conn.connect();

        assertEquals("Should return ZICO_AUTH_ERROR.",
                AbstractZicoConnector.ZICO_AUTH_ERROR, conn.sendData(new HelloRequest(1, "test", "BAD")));

        conn.close();
    }

    // TODO Transfer trace test

    // TODO bad CRC test

    // TODO reconnection test

    // TODO same context two connections test (+ two various symbol settings)
}
