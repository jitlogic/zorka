package com.jitlogic.zorka.common.test.http;

import com.jitlogic.zorka.common.http.*;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;


public class HttpServerTest {

    private static HttpServer server;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new HttpServer("test", "127.0.0.1", 0, new HttpConfig(),
            new HttpHandler() {
                @Override
                public HttpMessage handle(HttpMessage message) {
                    System.err.println("REQ: " + message.getUri());
                    return HttpMessage.RESP(200, "OK");
                }
            }, Executors.newFixedThreadPool(2), null);

        server.start();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stop();
        server = null;
    }

    @Test
    public void testHttpRequestResponse() throws Exception {
        HttpClient cli = new HttpClient(new HttpConfig(), "http://localhost:" + server.getLocalPort(), new MethodCallStatistics());
        HttpMessage msg = cli.handle(HttpMessage.GET("a/b"));
        assertEquals("OK", msg.getBodyAsString());
        assertEquals(200, msg.getStatus());
    }
}
