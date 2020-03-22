/*
 * Copyright (c) 2012-2020 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.common.http;

import com.jitlogic.zorka.common.tracedata.PerfTextChunk;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import static com.jitlogic.zorka.common.http.HttpProtocol.REG_URL_QSTR;
import static com.jitlogic.zorka.common.util.ZorkaConfig.parseInt;


/**
 * Generic HTTP output for text data. Accepts strings that will be
 */
public class HttpTextOutput extends ZorkaAsyncThread<PerfTextChunk> {

    private String url;
    private String uri;
    private Map<String,String> urlParams;
    private Map<String,String> headers = new TreeMap<String, String>();

    private HttpClient client;

    public HttpTextOutput(String name, Map<String,String> conf, Map<String,String> urlParams,
                          Map<String,String> headers, MethodCallStatistics stats) {
        super(name, parseInt(conf.get("qlen"), 256, name + "qlen should be a number."), 2);

        this.url = conf.get("url");

        this.urlParams = urlParams != null ? urlParams : new HashMap<String, String>();

        if (headers != null) {
                this.headers.putAll(headers);
        }

        HttpConfig config = new HttpConfig();

        if (url.toLowerCase().startsWith("https:")) {
            SSLContext ctx = TlsContextBuilder.fromMap("", conf);
            config.setSslContext(ctx);
        }

        this.client = new HttpClient(config, this.url, stats);

        StringBuilder sb = new StringBuilder();

        Matcher m = HttpProtocol.RE_URL.matcher(url);

        if (!m.matches()) {
            throw new ZorkaRuntimeException("Invalid URL: " + url);
        }

        String qstr = m.group(REG_URL_QSTR);

        if (qstr != null) {
            sb.append(qstr);
        }

        for (Map.Entry<String,String> e : this.urlParams.entrySet()) {
            sb.append(sb.length() == 0 ? '?' : '&');
            sb.append(ZorkaUtil.urlEncode(e.getKey()));
            sb.append('=');
            sb.append(ZorkaUtil.urlEncode(e.getValue()));
        }

        this.uri = sb.toString();

        log.info("Local URI = " + this.uri);
    }


    @Override
    protected void process(List<PerfTextChunk> msgs) {
        for (PerfTextChunk p : msgs) {
            try {
                HttpMessage req = HttpMessage.POST(uri, p.getData());

                for (Map.Entry<String,String> e : headers.entrySet()) {
                    req.header(e.getKey(), e.getValue());
                }
                
                HttpMessage res = client.handle(req);

                // TODO what about 302 ?
                if (res.getStatus() >= 400) {
                    log.warn(url + "  (" + uri + "): " + res.getStatus() + ": '" + res.getBodyAsString() + "'");
                    if (log.isDebugEnabled()) {
                        log.debug(url + ": request: '" + req.getBodyAsString() + "'");
                        log.debug(url + ": response: '" + res.getBodyAsString() + "'");
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("HTTP: {} -> {}", url, res.getStatus());
                }
            } catch (Exception e) {
                log.error("Error sending HTTP request", e);
            }
        }
    }
}
