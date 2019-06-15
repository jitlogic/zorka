package com.jitlogic.zorka.common.test.http;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.common.http.HttpProtocol.*;

public class HttpProtocolRegexUnitTest {

    public void are(Pattern re, String s, String...gs) {
        Matcher m = re.matcher(s);

        assertTrue("Doesn't match: s='" + s + "' re='" + re + "'", m.matches());
        int ngroups = m.groupCount();
        assertTrue("Missing groups: s='" + s + "' re='" + re + "'", ngroups <= gs.length+1);

        for (int i = 0; i < gs.length; i++) {
            assertEquals("Group " + i + " s='" + s + "', re='" + re + "'", gs[i], m.group(i+1));
        }
    }

    @Test
    public void testUrlMatcher() {
        are(RE_URL,"http://127.0.0.1:19005", "http", "127.0.0.1", ":19005", null);
        are(RE_URL,"http://localhost", "http", "localhost", null, null);
        are(RE_URL,"https://foobar.com/blah", "https", "foobar.com", null, "/blah");
        are(RE_URL,"https://foobar.com/blah?foo=bar", "https", "foobar.com", null, "/blah", "?foo=bar");
    }

    @Test
    public void testReqMatcher() {
        are(RE_REQ_LINE, "GET / HTTP/1.1", "GET", "/", null, "HTTP/1.1");
        are(RE_REQ_LINE, "GET /css/zico.css HTTP/1.1", "GET", "/css/zico.css", null, "HTTP/1.1");
        are(RE_REQ_LINE, "GET /view/mon/trace/list?text=SQL%20bromba HTTP/1.1", "GET", "/view/mon/trace/list",
                "?text=SQL%20bromba", "HTTP/1.1");
    }

}