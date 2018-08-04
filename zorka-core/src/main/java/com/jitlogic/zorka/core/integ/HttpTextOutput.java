/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic HTTP output for text data. Accepts strings that will be
 */
public class HttpTextOutput extends ZorkaAsyncThread<String> {

    private static Logger log = LoggerFactory.getLogger(HttpTextOutput.class);

    private String url, fullUrl;
    private Map<String,String> urlParams;
    private Map<String,String> headers;

    public HttpTextOutput(String name, Map<String,String> conf, Map<String,String> urlParams, Map<String,String> headers) {
        super(name);

        this.url = conf.get("url");

        this.urlParams = urlParams != null ? urlParams : new HashMap<String, String>();
        this.headers = headers != null ? headers : new HashMap<String, String>();

        makeFullUrl();
    }


    private void makeFullUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(url);

        if (urlParams.size() > 0) {
            int n = 0;
            for (Map.Entry<String,String> e : urlParams.entrySet()) {
                sb.append(n == 0 ? '?' : '&'); n++;
                sb.append(ZorkaUtil.urlEncode(e.getKey()));
                sb.append('=');
                sb.append(ZorkaUtil.urlEncode(e.getValue()));
            }
        }

        this.fullUrl = sb.toString();
    }


    @Override
    protected void process(List<String> msgs) {
        for (String msg : msgs) {
            try {
                HttpRequest req = HttpUtil.POST(fullUrl, msg);
                if (headers != null) {
                    req.getHeaders().putAll(headers);
                }
                HttpResponse res = req.go();

                // TODO what about 302 ?
                if (res.getStatus() >= 400) {
                    log.warn(url + ": error " + res.getStatus() + ": " + res.getStatusMsg());
                    if (log.isDebugEnabled()) {
                        log.debug(url + ": request: '" + req.getBodyAsString() + "'");
                        log.debug(url + ": response: '" + res.getBodyAsString() + "'");
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("HTTP: " + fullUrl + " -> " + res.getStatus() + " " + res.getStatusMsg());
                }
            } catch (IOException e) {
                log.error("Error sending HTTP request", e);
            }
        }
    }
}
