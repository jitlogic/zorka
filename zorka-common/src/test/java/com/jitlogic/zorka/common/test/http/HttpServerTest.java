package com.jitlogic.zorka.common.test.http;

import com.jitlogic.zorka.common.http.*;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;


public class HttpServerTest {

    private static HttpServer server;

    @BeforeClass
    public static void startServer() throws Exception {
        server = new HttpServer("test", "127.0.0.1", 19001, new HttpConfig(),
            new HttpHandler() {
                @Override
                public HttpMessage handle(HttpMessage message) {
                    System.err.println("REQ: " + message.getUri());
                    return HttpMessage.RESP(200, "OK");
                }
            }, Executors.newFixedThreadPool(2));

        server.start();
    }

    @Test
    public void testHttpRequestResponse() throws Exception {
        HttpClient cli = new HttpClient(new HttpConfig(), "http://localhost:19001", new MethodCallStatistics());
        HttpMessage msg = cli.handle(HttpMessage.GET("a/b"));
        assertEquals("OK", msg.getBodyAsString());
        assertEquals(200, msg.getStatus());
    }
}
