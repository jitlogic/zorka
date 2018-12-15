package com.jitlogic.netkit.test.unit;

import com.jitlogic.netkit.http.*;
import com.jitlogic.netkit.test.support.TestBufOutput;
import com.jitlogic.netkit.test.support.TestMessageListener;
import com.jitlogic.netkit.test.support.TestNetExchange;

import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRequestHandlerUnitTest {

    private HttpConfig config = new HttpConfig();
    private TestMessageListener cliListener = new TestMessageListener();
    private HttpMessageHandler  cliHandler = new HttpMessageHandler(config, cliListener);
    private TestMessageListener svrListener = new TestMessageListener();
    private HttpProtocolHandler serverHandler = new HttpProtocolHandler(
            config, new HttpMessageHandler(config, svrListener));

    private TestBufOutput serverConn = new TestBufOutput(serverHandler);
    private TestBufOutput clientConn = new TestBufOutput(
            new HttpProtocolHandler(config,
                    HttpDecoderState.READ_RESP_LINE,
                    new HttpMessageHandler(config, cliListener)));

    private TestNetExchange exchange = new TestNetExchange(clientConn, serverConn);


    @Test
    public void testSingleGetReq() {
        svrListener.addReplies(HttpMessage.RESP(200, null).headers("X-Foo", "bar"));
        exchange.submit(new HttpRequest(config, cliHandler, "http://localhost", HttpMethod.GET));
        assertEquals(1, svrListener.getReqs().size());
        assertEquals(1, cliListener.getReqs().size());

        assertEquals(0, serverConn.getNumCloses());
        assertEquals(0, clientConn.getNumCloses());
    }

    @Test
    public void testTwoReqs() {
        svrListener.addReplies(HttpMessage.RESP(200).headers("X-Foo", "bar"));
        svrListener.addReplies(HttpMessage.RESP(200).headers("X-Foo", "bar"));
        exchange.submit(new HttpRequest(config, cliHandler, "http://localhost", HttpMethod.GET));
        exchange.submit(new HttpRequest(config, cliHandler, "http://localhost", HttpMethod.GET));
        assertEquals("server",2, svrListener.getReqs().size());
        assertEquals("client", 2, cliListener.getReqs().size());

    }

    // TODO malformed request line - proper handling
}
