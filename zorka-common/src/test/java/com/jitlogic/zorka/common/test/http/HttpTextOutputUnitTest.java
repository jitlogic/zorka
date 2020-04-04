package com.jitlogic.zorka.common.test.http;

import com.jitlogic.zorka.common.http.*;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.test.support.CommonFixture;
import com.jitlogic.zorka.common.tracedata.PerfTextChunk;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HttpTextOutputUnitTest extends CommonFixture {

    private final static List<HttpMessage> httpReqs = new ArrayList<HttpMessage>();
    private final static List<HttpMessage> httpResps = new ArrayList<HttpMessage>();

    private static HttpServer httpServer;

    private static HttpMessage handleReq(HttpMessage msg) {
        synchronized (httpReqs) {
            httpReqs.add(msg);
        }
        synchronized (httpResps) {
            if (httpResps.size() > 0) {
                HttpMessage rsp = httpResps.get(0);
                httpResps.remove(0);
                return rsp;
            } else {
                return HttpMessage.RESP(200, "OK");
            }
        }
    }

    @BeforeClass
    public static void startServer() throws Exception {
        httpServer = new HttpServer("test", "127.0.0.1", 0, new HttpConfig(),
            new HttpHandler() {
                @Override
                public HttpMessage handle(HttpMessage message) {
                    return handleReq(message);
                }
            }, Executors.newFixedThreadPool(2), null);
        httpServer.start();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        httpServer.stop();
        httpServer = null;
    }

    @Test
    public void testSendHttpSampleText() {
        HttpTextOutput output = new HttpTextOutput(
            "test",
            ZorkaUtil.<String,String>map("qlen", "0",
                "url", "http://localhost:" + httpServer.getLocalPort() + "/write"),
            ZorkaUtil.<String,String>map("foo", "bar"),
            ZorkaUtil.<String,String>map("Foo", "Bar"),
            new MethodCallStatistics()
        );

        output.start();

        output.submit(new PerfTextChunk("test", "FUBAR".getBytes()));

        synchronized (httpReqs) {
            assertEquals(1, httpReqs.size());
            assertEquals("/write", httpReqs.get(0).getUri());
        }
    }
}
