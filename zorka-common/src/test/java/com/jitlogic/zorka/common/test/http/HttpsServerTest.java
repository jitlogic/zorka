package com.jitlogic.zorka.common.test.http;

import com.jitlogic.zorka.common.http.HttpServer;
import com.jitlogic.zorka.common.util.TlsContextBuilder;
import com.jitlogic.zorka.common.http.HttpConfig;
import com.jitlogic.zorka.common.http.HttpMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpsServerTest {

    private static HttpConfig config = new HttpConfig();

    private static SSLContext sslctx;
    private static HttpServer server;


    @BeforeClass
    public static void startServer() throws Exception {


        sslctx = TlsContextBuilder.svrContext("src/test/resources/tls/localhost.jks", "changeit");

        SSLContext.setDefault(sslctx);

//        server = new NetServer("test-server", "127.0.0.1", 19008,
//                new BufHandlerFactory() {
//                    @Override
//                    public BufHandler create(SocketChannel ch) {
//                        //return new HttpProtocolHandler(config, new RingHandler(config, ringFn, Executors.newSingleThreadExecutor()));
//                    }
//                },
//                sslctx);
//        server.start();
    }


    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }


    public static HttpMessage GET(String strUrl) throws Exception {
        URL url = new URL(strUrl);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream is = conn.getInputStream();
        Map<String, List<String>> headers = conn.getHeaderFields();
        HttpMessage resp = new HttpMessage(true);
        resp.setStatus(conn.getResponseCode());
        for (Map.Entry<String,List<String>> e : headers.entrySet()) {
            for (String v : e.getValue()) {
                resp.header(e.getKey(), v);
            }
        }
        is.close();
        conn.disconnect();
        return resp;
    }


    @Test
    public void testHttpsRequest() throws Exception {
//        HttpMessage msg1 = GET("https://localhost:19008");
//        assertEquals(200, msg1.getStatus());
//        HttpMessage msg2 = GET("https://localhost:19008");
//        assertEquals(201, msg2.getStatus());
//        HttpMessage msg3 = GET("https://localhost:19008");
//        assertEquals(202, msg3.getStatus());
    }
}
