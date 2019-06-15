package com.jitlogic.netkit.test.integ;

import com.jitlogic.netkit.BufHandler;
import com.jitlogic.netkit.BufHandlerFactory;
import com.jitlogic.netkit.NetServer;
import com.jitlogic.netkit.tls.TlsContextBuilder;
import com.jitlogic.netkit.test.support.EchoHandler;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.channels.SocketChannel;

public class TlsServerTest {

    private static SSLContext sslctx;
    private static NetServer server;

    private static EchoHandler handler = new EchoHandler(false);

    @BeforeClass
    public static void startServer() throws Exception {
        //System.setProperty("javax.net.debug", "all");
        sslctx = TlsContextBuilder.svrContext("src/test/resources/tls/localhost.jks", "changeit");
        server = new NetServer("test-server", "127.0.0.1", 19007, new BufHandlerFactory() {
            @Override
            public BufHandler create(SocketChannel ch) {
                return handler;
            }
        }, sslctx, new MethodCallStatistics());
        server.start();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop(1000);
            server = null;
        }
    }

    @Test
    public void testTlsConnSingleRoundtrip() throws Exception {
        SSLSocketFactory sf = sslctx.getSocketFactory();

        SSLSocket conn = (SSLSocket) sf.createSocket("127.0.0.1", 19007);
        conn.startHandshake();

        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(conn.getOutputStream())));

        out.println("ojaaa!");
        out.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line = in.readLine();

        assertEquals("ojaaa!", line);

        //System.out.println(line);

        out.println("br0mba");
        out.flush();

        line = in.readLine();

        assertEquals("br0mba", line);

        in.close(); out.close(); conn.close();
    }
}
