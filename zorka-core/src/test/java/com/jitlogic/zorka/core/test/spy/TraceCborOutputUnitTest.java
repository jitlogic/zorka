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

import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.CborTraceOutput;
import com.jitlogic.zorka.core.test.support.TestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import static org.junit.Assert.*;

public class TraceCborOutputUnitTest extends ZorkaFixture {

    private int sid(String symbol) {
        return symbols.symbolId(symbol);
    }


    private Symbol sym(String name) {
        return new Symbol(symbols.symbolId(name), name);
    }


    private Symbol sym(int id, String name) {
        return new Symbol(id, name);
    }

    public TraceRecord tr(String className, String methodName, String methodSignature,
                          long calls, long errors, int flags, long time,
                          TraceRecord... children) {
        TraceRecord tr = new TraceRecord(null);
        tr.setClassId(sid(className));
        tr.setMethodId(sid(methodName));
        tr.setSignatureId(sid(methodSignature));
        tr.setCalls(calls);
        tr.setErrors(errors);
        tr.setFlags(flags);
        tr.setTime(time);

        for (TraceRecord child : children) {
            child.setParent(tr);
            tr.addChild(child);
        }

        return tr;
    }

    @Test
    public void testOutputAuth() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
            "http://zorka.io/", "123", "secret",
            "myapp.local", "myapp", "TST");

        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/auth")).response(200, "someUUID");

        output.authenticate();

        assertEquals("someUUID", TestUtil.getField(output, "sessionUUID"));

        httpClient.verify();
    }

    @Test
    public void testPostSimpleTrace() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
            "http://zorka.io", "123", "secret",
            "myapp.local", "myapp", "TST");

        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle();

        httpClient.verify();
    }

    @Test
    public void testPostTwoTracesOneAgd() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
            "http://zorka.io/", "123", "secret",
            "myapp.local", "myapp", "TST");

        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();
    }

    @Test
    public void testSwitchSessionAndRepost() throws Exception {
        CborTraceOutput output = (CborTraceOutput) tracer.toCbor(
            "http://zorka.io/", "123", "secret",
            "myapp.local", "myapp", "TST");

        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(412, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/auth")).response(200, "someUUID");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/agd")).response(202, "OK");
        httpClient.expect(HttpUtil.GET("http://zorka.io/agent/submit/trc")).response(202, "OK");

        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.submit(tr("some.class", "someMethod", "()V", 1, 0, 0, 100));
        output.runCycle(); output.runCycle();

        httpClient.verify();

    }

    // TODO implement and test merged posts
}
