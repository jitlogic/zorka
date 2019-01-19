/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.ltracer.LTraceHttpOutput;
import com.jitlogic.zorka.core.test.support.CoreTestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.netkit.http.HttpMessage.*;

public class TraceCborOutputUnitTest extends ZorkaFixture {

    private String map2json(Object...args) {
        return new JSONWriter().write(ZorkaUtil.constMap(args));
    }

    @Test
    public void testRegisterNewAgent() {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "auth.key", "secret",
                        "hostname", "zorka.myapp",
                        "app.name", "myapp",
                        "env.name", "TST"));
        ObjectInspector.setField(output, "httpClient", httpClient);
        httpClient.expect(
                POST("/agent/register",
                    map2json("rkey", "secret",
                        "name", "zorka.myapp", "app", "myapp", "env", "TST"),
                        "Content-Type", "application/json"))
                .setResponse(
                        RESP(201, map2json("id", "123", "authkey", "secret")));

        output.register();

        httpClient.verify();
        assertTrue("Agent UUID should be received by agent.",
                output.getAgentUUID().length() > 0);
    }

    @Test(expected = ZorkaRuntimeException.class)
    public void testRegisterAgentWithBadAuthKey() {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
            ZorkaUtil.<String,String>constMap(
                "http.url", "http://zorka.io/",
                "auth.key", "bad",
                "hostname", "zorka.myapp",
                "app.name", "myapp",
                "env.name", "TST"));
        ObjectInspector.setField(output, "httpClient", httpClient);
        httpClient.expect(
                POST("/agent/register",
                        map2json("rkey", "bad", "name", "zorka.myapp",
                                "app", "myapp", "env", "TST"),
                        "Content-Type", "application/json"))
                .setResponse(RESP(401, map2json("error", "Permission denied.")));

        output.register();

        httpClient.verify();
    }

    @Test @Ignore
    public void testOutputSession() throws Exception {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "agent.id", "123",
                        "sessn.key", "secret",
                        "hostname", "zorka.myapp",
                        "app.name", "myapp",
                        "env.name", "TST"
                ));

        httpClient.expect(
                POST("http://zorka.io/agent/session",
                        map2json("uuid", "123", "authkey", "secret")))
                .setResponse(RESP(200, map2json("session", "someUUID")));


        output.openSession();

        assertEquals("someUUID",
                CoreTestUtil.getField(output, "sessionUUID"));

        httpClient.verify();
    }

    @Test @Ignore
    public void testPostSimpleTrace() {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth"))
                .setResponse(RESP(200, "someUUID"));
        httpClient.expect(GET("http://zorka.io/agent/submit/agd"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(202, "OK"));

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle();

        httpClient.verify();
    }

    @Test @Ignore
    public void testPostTwoTracesOneAgd() {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                "http.url", "http://zorka.io/",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth"))
                .setResponse(RESP(200, "someUUID"));
        httpClient.expect(GET("http://zorka.io/agent/submit/agd"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(202, "OK"));

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();
    }

    @Test @Ignore
    public void testSwitchSessionAndRepost() throws Exception {
        LTraceHttpOutput output = (LTraceHttpOutput) tracer.toCbor(
                ZorkaUtil.<String,String>constMap(
                        "http.url", "http://zorka.io/",
                        "agent.uuid", "123",
                        "auth.key", "secret",
                        "hostname", "myapp.local",
                        "app.name", "myapp",
                        "env.name", "TST"));

        httpClient.expect(GET("http://zorka.io/agent/auth"))
                .setResponse(RESP(200, "someUUID"));
        httpClient.expect(GET("http://zorka.io/agent/submit/agd"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(412, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/auth"))
                .setResponse(RESP(200, "someUUID"));
        httpClient.expect(GET("http://zorka.io/agent/submit/agd"))
                .setResponse(RESP(202, "OK"));
        httpClient.expect(GET("http://zorka.io/agent/submit/trc"))
                .setResponse(RESP(202, "OK"));

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();

    }

    // TODO implement and test merged posts
}
