package com.jitlogic.netkit.test.integ;

import com.jitlogic.netkit.*;
import com.jitlogic.netkit.http.*;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static com.jitlogic.netkit.http.HttpProtocol.*;

public class HttpServerTest {

    private static class SvcListener implements HttpListener {
        @Override
        public SvcListener request(SelectionKey key, String httpVersion, HttpMethod method, String url, String query) {
            System.out.println("SVR|REQ: version=" + httpVersion + ", method=" + method + ", url=" + url + ", query=");
            return this;
        }

        @Override
        public SvcListener response(SelectionKey key, String httpVersion, int status, String statusMessage) {
            System.out.println("SVR|RESP: version=" + httpVersion + ", status=" + status + ", message=" + statusMessage);
            return this;
        }

        @Override
        public SvcListener header(SelectionKey key, String name, String value) {
            System.out.println("SVR|HEADER|" + name + ": " + value);
            return this;
        }

        @Override
        public SvcListener body(SelectionKey key, Object...parts) {
            for (Object o : parts) {
                ByteBuffer b = (ByteBuffer)o;
                System.out.println("SVR|Body: len=" + b.remaining());
            }
            return this;
        }

        @Override
        public SvcListener finish(SelectionKey key) {
            System.out.println("SVR|Finish.");
            HttpEncoder encoder = new HttpEncoder(config);
            encoder.response(key, HTTP_1_1, 200, "");
            encoder.header(key, "X-Some-Header", "someVal");
            encoder.body(key, ByteBuffer.wrap("BAR".getBytes()));
            encoder.finish(key);
            return this;
        }

        @Override
        public SvcListener error(SelectionKey key, int status, String message, Object data, Throwable e) {
            System.out.println("SVR|ERROR: " + message + "| '" + data + "'");
            return this;
        }
    }



    private static class CliListener implements HttpListener {
        @Override
        public CliListener request(SelectionKey key, String httpVersion, HttpMethod method, String url, String query) {
            System.out.println("CLI|REQ: version=" + httpVersion + ", method=" + method + ", url=" + url + ", query=");
            return this;
        }

        @Override
        public CliListener response(SelectionKey key, String httpVersion, int status, String statusMessage) {
            System.out.println("CLI|RESP: version=" + httpVersion + ", status=" + status + ", message=" + statusMessage);
            return this;
        }

        @Override
        public CliListener header(SelectionKey key, String name, String value) {
            System.out.println("CLI|HEADER|" + name + ": " + value);
            return this;
        }

        @Override
        public CliListener body(SelectionKey key, Object...parts) {
            for (Object obj : parts) {
                ByteBuffer body = (ByteBuffer)obj;
                System.out.println("CLI|Body: len=" + body.remaining());
            }
            return this;
        }

        @Override
        public CliListener finish(SelectionKey key) {
            System.out.println("CLI|Finish.");
            return this;
        }

        public CliListener error(SelectionKey key, int status, String message, Object data, Throwable e) {
            System.out.println("CLI|ERROR: " + message + "| '" + data + "'");
            return this;
        }
    }

    private static SvcListener listener = new SvcListener();
    private static NetServer server;
    private static NetClient client;
    private static HttpConfig config = new HttpConfig();

    @BeforeClass
    public static void startServer() throws Exception {
//        server = new NetServer("test-server", "127.0.0.1", 19001,
//                new BufHandlerFactory() {
//                    @Override
//                    public BufHandler create(SocketChannel ch) {
//                        return new HttpProtocolHandler(new HttpConfig(), new SvcListener());
//                    }
//                },
//                null, new MethodCallStatistics());
//        server.start();
//
//        client = new NetClient("test-client", NetCtxFactory.DEFAULT, null, new MethodCallStatistics());
//        client.start();
    }

    @Test
    public void testHttpRequestResponse() throws Exception {
//        HttpListener lsnr = new CliListener();
//        HttpRequest req = new HttpRequest(config, lsnr, "http://localhost:19001")
//                .header("X-Foo", "Bar");
//        client.submit(req);
//        Thread.sleep(1000);
    }
}
