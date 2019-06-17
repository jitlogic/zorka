/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.common.http.HttpConfig;
import com.jitlogic.zorka.common.http.HttpMessage;
import com.jitlogic.zorka.common.http.HttpClient;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Random;

import static com.jitlogic.zorka.cbor.HttpConstants.*;
import static com.jitlogic.zorka.common.util.ZorkaConfig.parseInt;

public abstract class ZicoHttpOutput extends ZorkaAsyncThread<SymbolicRecord> {

    protected String sessionID;

    protected boolean isClean = true;

    protected String submitTraceUrl, submitAgentUrl;

    protected int retries, timeout;
    protected long retryTime, retryTimeExp;

    protected SymbolRegistry registry;

    protected ZorkaConfig config;

    protected HttpConfig httpConfig;
    protected HttpClient httpClient;

    protected Random rand = new Random();

    public ZicoHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry, MethodCallStatistics stats) {
        super("ZORKA-CBOR-OUTPUT", parseInt(conf.get("http.qlen"), 64, "tracer.http.qlen"), 1);

        this.config = config;

        String url = "/agent/";

        this.submitAgentUrl = url + "submit/agd";
        this.submitTraceUrl = url + "submit/trc";

        this.retries = parseInt(conf.get("http.retries"), 10, "tracer.http.retries");
        this.retryTime = parseInt(conf.get("http.retry.time"), 125, "tracer.http.retry.time");
        this.retryTimeExp = parseInt(conf.get("http.retry.exp"), 2, "tracer.http.retry.exp");
        this.timeout = parseInt(conf.get("http.timeout"), 60000, "tracer.http.output");
        this.registry = registry;

        this.httpConfig = new HttpConfig();
        httpConfig.setKeepAliveTimeout(timeout);

        this.httpClient = HttpClient.fromMap(conf, stats);

        this.sessionID = String.format("%016x", rand.nextLong());
    }

    protected void send(byte[] body, int bodyLength, String uri, long traceId1, long traceId2, boolean reset) {
        HttpMessage req = HttpMessage.POST(uri, ZorkaUtil.clipArray(body, 0, bodyLength),  // TODO this is inefficient
                HDR_ZORKA_SESSION_ID, sessionID,
                HDR_ZORKA_SESSION_RESET, ""+reset,
                "Content-Type", ZORKA_CBOR_CONTENT_TYPE);
        if (traceId1 != 0 && traceId2 != 0) {
            req.header("X-Zorka-Trace-ID", ZorkaUtil.hex(traceId1, traceId2));
        }
        HttpMessage res = httpClient.handle(req);
        if (res.getStatus() < 300) {
            if (log.isTraceEnabled()) log.trace("Submitted: " + uri + " : " + ZorkaUtil.hex(traceId1, traceId2));
        } else if (res.getStatus() == 412) {
            throw new ZorkaRuntimeException("Resend.");
        } else {
            if (log.isTraceEnabled()) {
                log.trace("ERROR at send(): uri=" + uri + ", status=" + res.getStatus() + ", data=" + ZorkaUtil.hex(body, bodyLength)
                    + ": " + new String(body, 0, bodyLength, Charset.defaultCharset()));
            }
            throw new ZorkaRuntimeException("Server error: " + res.getStatus() + " " + uri);
        }
    }

}
