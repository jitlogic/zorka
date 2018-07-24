/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.http.HttpClient;
import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.http.MiniHttpClient;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.CborTraceOutput;
import com.jitlogic.zorka.core.test.support.CoreTestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class TraceCborOutputUnitTest extends ZorkaFixture {

    private HttpRequest GET(String url) throws IOException {
        return HttpUtil.GET(url).withHeader("Host", "zorka.io");
    }

    private HttpRequest POST(String url, String body) throws IOException {
        return HttpUtil.POST(url, "");
    }

    private String map2json(Object...args) {
        return new JSONWriter().write(ZorkaUtil.constMap(args));
    }

    @Test
    public void testRegisterNewAgent() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "auth.key", "secret",
                        "hostname", "zorka.myapp",
                        "app.name", "myapp",
                        "env.name", "TST"
                        //"attrs.location", "DC1",
                        //"attrs.room", "1A",
                        //"attrs.rack", "12"
                ));
        httpClient.expect(
                POST("http://zorka.io/agent/register",
                    map2json("rkey", "secret",
                        "name", "zorka.myapp", "app", "myapp", "env", "TST"))
                    .withHeader("Content-Type", "application/json"))
        .response(201, map2json("uuid", "123", "authkey", "secret"));

        output.register();

        httpClient.verify();
        assertTrue("Agent UUID should be received by agent.",
                output.getAgentUUID().length() > 0);
    }

    //@Test
    public void testRegisterNewAgentWithAttrs() throws Exception {
        // TODO
    }

    @Test(expected = ZorkaRuntimeException.class)
    public void testRegisterAgentWithBadAuthKey() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
            ZorkaUtil.<String,String>constMap(
                "http.url", "http://zorka.io/",
                "auth.key", "bad",
                "hostname", "zorka.myapp",
                "app.name", "myapp",
                "env.name", "TST"
                //"attrs.location", "DC1",
                //"attrs.room", "1A",
                //"attrs.rack", "12"
            ));
        httpClient.expect(
                POST("http://zorka.io/agent/register",
                        map2json("rkey", "bad", "name", "zorka.myapp",
                                "app", "myapp", "env", "TST"))
                        .withHeader("Content-Type", "application/json"))
                .response(401, map2json("error", "Permission denied."));

        output.register();
    }

    public void testRegisterAgentWithAlreadyExistingKey() throws Exception {
        // TODO
    }

    public void testRegisterNewAndReregisterAgainWithObtainedKey() throws Exception {
        // TODO
    }

    @Test @Ignore
    public void testOutputSession() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "agent.uuid", "123",
                        "sessn.key", "secret",
                        "hostname", "zorka.myapp",
                        "app.name", "myapp",
                        "env.name", "TST"
                ));

        httpClient.expect(
                POST("http://zorka.io/agent/session",
                        map2json("uuid", "123", "authkey", "secret"))
                ).response(200, map2json("session", "someUUID"));


        output.openSession();

        assertEquals("someUUID",
                CoreTestUtil.getField(output, "sessionUUID"));

        httpClient.verify();
    }

    @Test @Ignore("This requires working ZICO 2.x collector.")
    public void testOutputAuthManual() {
        HttpClient miniClient = new MiniHttpClient();
        ObjectInspector.setField(HttpUtil.class, "client", miniClient);

        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://127.0.0.1:8640/",
                        "agent.uuid", "21c00000-0201-0000-0015-deadbeef1003",
                        "auth.key", "deadbeefdadacafe",
                        "hostname", "myapp.local",
                        "app.name", "MYAPP2",
                        "env.name", "PRD"));

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));

        output.runCycle();
    }

    @Test @Ignore
    public void testPostSimpleTrace() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle();

        httpClient.verify();
    }

    @Test @Ignore
    public void testPostTwoTracesOneAgd() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                "http.url", "http://zorka.io/",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();
    }

    @Test @Ignore
    public void testSwitchSessionAndRepost() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(412, "OK");
        httpClient.expect(GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();

    }

    // TODO implement and test merged posts
}
