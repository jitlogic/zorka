package com.jitlogic.zorka.net;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.net.http.nano.HttpConnectionHandler;
import com.jitlogic.zorka.net.http.nano.IHTTPSession;
import com.jitlogic.zorka.net.http.nano.Response;
import com.jitlogic.zorka.net.http.nano.Status;
import com.jitlogic.zorka.net.http.nano.IHandler;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.impl.ConsoleTrapper;
import org.slf4j.impl.ZorkaLoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SimpleHttpServiceTest {

    private static Executor executor = Executors.newSingleThreadExecutor();

    private static class TestHttpHandler implements IHandler<IHTTPSession,Response> {

        @Override
        public Response handle(IHTTPSession input) {
            return Response.newFixedLengthResponse(Status.OK, "text/plain", "BORK!");
        }
    }

    @Test @Ignore
    public void testServeSimpleHttpReq() throws Exception {
        ZorkaLoggerFactory.getInstance().swapTrapper(new ConsoleTrapper());
        TcpService svc = new TcpService(new ZorkaConfig(), executor,
                new HttpConnectionHandler(new TestHttpHandler()),
                "test", "127.0.0.1", 10000);
        svc.restart();
        Thread.sleep(120000);
    }

    @Test
    public void testServeSimpleHttpsReq() throws Exception {
        ZorkaConfig zc = new ZorkaConfig();
        zc.setCfg("test.tls", "yes");
        zc.setCfg("test.tls.keystore", this.getClass().getResource("/tls/localhost.jks").getPath());
        zc.setCfg("test.tls.keystore.pass", "changeit");

        ZorkaLoggerFactory.getInstance().swapTrapper(new ConsoleTrapper());
        TcpService svc = new TcpService(zc, executor,
                new HttpConnectionHandler(new TestHttpHandler()),
                "test", "127.0.0.1", 10000);
        svc.restart();
        Thread.sleep(120000);

    }
}
