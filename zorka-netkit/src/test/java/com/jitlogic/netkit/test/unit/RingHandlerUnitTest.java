package com.jitlogic.netkit.test.unit;

import com.jitlogic.netkit.http.*;
import com.jitlogic.netkit.integ.ring.RingHandler;
import com.jitlogic.netkit.test.support.TestBufOutput;
import com.jitlogic.netkit.test.support.TestMessageListener;
import com.jitlogic.netkit.test.support.TestNetExchange;
import com.jitlogic.netkit.test.support.TestRingFn;
import com.jitlogic.netkit.util.NetkitUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class RingHandlerUnitTest {

    private HttpConfig config = new HttpConfig();
    private TestRingFn ringFn = new TestRingFn();
    private RingHandler ringHandler = new RingHandler(config, ringFn, NetkitUtil.RUN);

    private TestMessageListener cliListener = new TestMessageListener();
    private HttpMessageHandler cliHandler = new HttpMessageHandler(config, cliListener);

    private HttpProtocolHandler serverHandler = new HttpProtocolHandler(new HttpConfig(), ringHandler);

    private TestBufOutput serverConn = new TestBufOutput(serverHandler);
    private TestBufOutput clientConn = new TestBufOutput(
            new HttpProtocolHandler(config, HttpDecoderState.READ_RESP_LINE,
                    new HttpMessageHandler(config, cliListener)));

    private TestNetExchange exchange = new TestNetExchange(clientConn, serverConn);

    @Test
    public void testSingleGetReq() {
        ringFn.add(TestRingFn.resp(200, "BLAH"));
        exchange.submit(new HttpRequest(config, cliHandler, "http://localhost", HttpMethod.GET));

        assertEquals(1, ringFn.getReqs().size());
        assertEquals(1, cliListener.getReqs().size());

        //System.out.println(serverConn.toString());
        //System.out.println(clientConn.toString());
    }

    @Test
    public void testReqWithMultiHeaderValues() {
        ringFn.add(TestRingFn.resp(200, "BLAH"));
        exchange.submit(new HttpRequest(config, cliHandler, "http://localhost", HttpMethod.GET,
                "X-Foo", "bar", "X-Foo", "baz", "X-Foo", "bag"));

        assertEquals(1, ringFn.getReqs().size());
        assertEquals(1, cliListener.getReqs().size());
    }

    // TODO check for MapEntry entries when iterating over headers map

    // TODO check for ISeq-uable types as header values

}
