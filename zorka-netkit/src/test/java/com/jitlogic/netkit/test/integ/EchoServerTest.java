package com.jitlogic.netkit.test.integ;

import com.jitlogic.netkit.*;
import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.test.support.EchoHandler;
import com.jitlogic.netkit.test.support.EchoRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class EchoServerTest {

    private static NetServer server;
    private static NetClient client;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new NetServer("test-server", "127.0.0.1", 19005,
                new BufHandlerFactory() {
                    @Override
                    public BufHandler create(SocketChannel ch) {
                        return new EchoHandler(true);
                    }
                }, null);
        server.start();

        client = new NetClient("test-client",
                NetCtxFactory.DEFAULT,
                null);
        client.start();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop(1000);
            server = null;
        }

        if (client != null) {
            client.stop(1000);
            client = null;
        }
    }

    @Test
    public void testStartServer() throws Exception {
        LoggerFactory.setLevel(LoggerFactory.TRACE_LEVEL);
        EchoRequest request = new EchoRequest("127.0.0.1", 19005);
        client.submit(request);
        assertEquals("HELO\n", request.getReply());
    }

}
