package com.jitlogic.zorka.common.test.http;

import com.jitlogic.zorka.common.http.HttpConfig;
import com.jitlogic.zorka.common.http.HttpDecoder;
import com.jitlogic.zorka.common.http.HttpMethod;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/** Tests for encoder and decoder. */
public class HttpDecoderUnitTest {

//    private HttpConfig config = new HttpConfig();
//    private TestHttpListener l = new TestHttpListener();
//    private TestSelectionKey key = new TestSelectionKey("test");
//
//    @Test
//    public void testDecodeSimpleReq() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("GET / HTTP/1.1", "User-Agent: test/12.3", "");
//
//        assertTrue(d.hasInitial());
//        d.submit(key, false, b);
//        assertFalse(d.hasInitial());
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        l.check("request",  key, "HTTP/1.1", HttpMethod.GET, "/", null);
//        assertEquals(0, l.count("response"));
//        l.check("header", key, "User-Agent", "test/12.3");
//        assertEquals(0, l.count("body"));
//        l.check("finish", key);
//    }
//
//    @Test
//    public void testMalformedCRLF() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf0("GET / HTTP/1.1\r", "User-Agent: test/12.3\r\n", "\r\n");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//    }
//
//    @Test
//    public void testLongLine() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("GET /1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890 HTTP/1.1", "");
//
//        d.submit(key, false, b);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//    }
//
//    @Test
//    public void testDecodeTwoMsgsWithReset() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("GET / HTTP/1.1", "User-Agent: test/12.3", "");
//
//        d.submit(key, false, b);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals(1, l.count("request"));
//        assertEquals(1, l.count("finish"));
//
//        b.position(0);
//        d.reset();
//        d.submit(key, false, b);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals(2, l.count("request"));
//        assertEquals(2, l.count("finish"));
//    }
//
//    @Test
//    public void testDecodeTwoMsgsWithoutReset() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf(
//                "GET /a HTTP/1.1", "User-Agent: test/12.3", "",
//                "GET /b HTTP/1.1", "User-Agent: test/12.3", ""
//        );
//
//        d.submit(key, false, b);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//
//        assertEquals(2, l.count("request"));
//        assertEquals(2, l.count("finish"));
//    }
//
//    @Test
//    public void testPostWithFixedLengthBody() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("POST / HTTP/1.1", "Content-Length: 4", "", "ABCD");
//
//        d.submit(key, false, b);
//
//        assertFalse(d.hasError());
//
//        l.check("body", key);
//        assertEquals("ABCD", l.getBody());
//    }
//
//    @Test
//    public void testPostWithFixedFragmentedLengthBody() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b1 = sbuf0("POST / HTTP/1.1\r\n", "Content-Length: 8\r\n", "\r\n", "ABCD");
//        ByteBuffer b2 = sbuf0("EFGH");
//
//        d.submit(key, false, b1, b2);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//
//        assertEquals(1, l.count("body"));
//        assertEquals("ABCDEFGH", l.getBody());
//    }
//
//    @Test
//    public void testBodyTooLargeRquest() {
//        config.setMaxBodySize(4);
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf0("POST / HTTP/1.1\r\n", "Content-Length: 8\r\n", "\r\n", "ABCDEFGH");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        l.check("error", key, 400, "too large");
//    }
//
//    @Test
//    public void testFragmentedReq() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b1 = sbuf0("GET /");
//        ByteBuffer b2 = sbuf0(" HTTP/1.1\r\n", "User-Agent: ");
//        ByteBuffer b3 = sbuf0("test/12.3\r\n", "\r\n");
//
//        d.submit(key, false, b1, b2, b3);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        l.check("request",  key, "HTTP/1.1", HttpMethod.GET, "/", null);
//        assertEquals(0, l.count("response"));
//        l.check("header", key, "User-Agent", "test/12.3");
//        assertEquals(0, l.count("body"));
//        l.check("finish", key);
//    }
//
//    @Test
//    public void testParseMalformedRequestLine() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("BORK / HTTP/1.1", "User-Agent: test/12.3", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        assertEquals(0, l.count("request"));
//        l.check("error", key, 400, "malformed HTTP message", "BORK / HTTP/1.1");
//    }
//
//    @Test
//    public void testParseMalformedHeader() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//
//        ByteBuffer b = sbuf("GET / HTTP/1.1", "User-Agent test/12.3", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        assertEquals(1, l.count("request"));
//        l.check("error", key, 400, "malformed HTTP message", "User-Agent test/12.3");
//    }
//
//    @Test
//    public void testDecodeSimpleResp() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 201 OK", "Content-Length: 0", "");
//
//        assertTrue(d.hasInitial());
//        assertTrue(!d.hasFinished());
//        d.submit(key, false, b);
//        assertTrue(d.hasFinished());
//        assertFalse(d.hasInitial());
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals(0, l.count("request"));
//        l.check("response", key, "HTTP/1.1", 201, "OK");
//        l.check("header", key, "Content-Length", "0");
//        assertEquals(0, l.count("body"));
//        l.check("finish", key);
//    }
//
//    @Test
//    public void testFragmentedResp() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b1 = sbuf0("HTTP/1.1 20");
//        ByteBuffer b2 = sbuf0("1 OK\r\n", "Cont");
//        ByteBuffer b3 = sbuf0("ent-Length: 0\r\n\r\n");
//
//        d.submit(key, false, b1, b2, b3);
//
//        assertFalse(d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals(0, l.count("request"));
//        l.check("response", key, "HTTP/1.1", 201, "OK");
//        l.check("header", key, "Content-Length", "0");
//        assertEquals(0, l.count("body"));
//        l.check("finish", key);
//
//    }
//
//    @Test
//    public void testParseMalformedResponseLine() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 20a b0rk", "Content-Length: 0", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        assertEquals(0, l.count("response"));
//        l.check("error", key, 400, "malformed HTTP message", "HTTP/1.1 20a b0rk");
//    }
//
//    @Test
//    public void testChunkedResponse() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 200 OK", "Transfer-Encoding: chunked", "",
//                "0004;foo=bar", "ABCD",
//                "0004 ", "EFGH",
//                "0004\220", "IJKL",
//                "0", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(!d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals("ABCDEFGHIJKL", l.getBody());
//    }
//
//    @Test
//    public void testChunkedFragmentedResponse() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//
//        ByteBuffer b1 = sbuf0("HTTP/1.1 200 OK\r\n", "Transfer-Encoding: chunked\r\n", "\r\n", "0004\r\n", "AB");
//        ByteBuffer b2 = sbuf0("CD\r\n", "0\r\n", "\r\n");
//
//        d.submit(key, false, b1, b2);
//
//        assertTrue(!d.hasError());
//        assertTrue(d.hasFinished());
//        assertEquals("ABCD", l.getBody());
//    }
//
//    @Test
//    public void testChunkedResponseTooLarge() {
//        config.setMaxBodySize(8);
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 200 OK", "Transfer-Encoding: chunked", "",
//                "0004", "ABCD", "0004", "EFGH", "0004", "IJKL", "0", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        l.check("error", key, 400, "too large");
//    }
//
//    // TODO too long line
//
//    @Test
//    public void testTooLongLine() {
//        config.setMaxLineSize(10);
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 200 OK", "Transfer-Encoding: chunked", "",
//                "0004", "ABCD", "0004", "EFGH", "0004", "IJKL", "0", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        l.check("error", key, 400, "too large");
//    }
//
//    @Test
//    public void testChunkedMalformedResponse() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 200 OK", "Transfer-Encoding: chunked", "",
//                "0004x;foo=bar", "ABCD", "0", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        assertEquals(1, l.count("error"));
//    }
//
//    @Test
//    public void testResponseWithMalformedContentLength() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_RESP_LINE, l);
//        ByteBuffer b = sbuf("HTTP/1.1 200 OK", "Content-Length: b0rk", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(d.hasError());
//        assertEquals(1, l.count("error"));
//    }
//
//    @Test
//    public void testRequestWithCommaSeparatedHeader() {
//        HttpDecoder d = new HttpDecoder(config, HttpDecoderState.READ_REQ_LINE, l);
//        ByteBuffer b = sbuf("GET / HTTP/1.1", "X-Foo: bar,baz", "");
//
//        d.submit(key, false, b);
//
//        assertTrue(!d.hasError());
//        assertEquals(2, l.count("header"));
//    }
//
//    // TODO comma separated header values, quoted header values
//

}
