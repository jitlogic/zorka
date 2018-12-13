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

import com.jitlogic.netkit.http.*;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.jitlogic.netkit.http.HttpProtocol.REG_URL_PATH;
import static com.jitlogic.netkit.http.HttpProtocol.REG_URL_QSTR;


/**
 * Generic HTTP output for text data. Accepts strings that will be
 */
public class HttpTextOutput extends ZorkaAsyncThread<String> {

    private String url;
    private String uri;
    private Map<String,String> urlParams;
    private String[] headers;

    private HttpMessageClient client;

    public HttpTextOutput(String name, Map<String,String> conf, Map<String,String> urlParams, Map<String,String> headers) {
        super(name);

        this.url = conf.get("url");

        this.urlParams = urlParams != null ? urlParams : new HashMap<String, String>();

        if (headers != null) {
            this.headers = new String[headers.size()*2];
            int i = 0;
            for (Map.Entry<String,String> e : headers.entrySet()) {
                this.headers[i] = e.getKey();
                this.headers[i+1] = e.getValue();
                i += 2;
            }
        } else {
            this.headers = new String[0];
        }

        this.client = new HttpStreamClient(new HttpConfig(), this.url);

        parseUrl();
    }


    private void parseUrl() {
        StringBuilder sb = new StringBuilder();

        Matcher m = HttpProtocol.RE_URL.matcher(url);

        if (!m.matches()) {
            throw new ZorkaRuntimeException("Invalid URL: " + url);
        }

        String path = m.group(REG_URL_PATH);
        String qstr = m.group(REG_URL_QSTR);

        if (path == null) {
            path = "/";
        }

        if (qstr != null) {
            sb.append(qstr);
        }

        for (Map.Entry<String,String> e : urlParams.entrySet()) {
            sb.append(sb.length() == 0 ? '?' : '&');
            sb.append(ZorkaUtil.urlEncode(e.getKey()));
            sb.append('=');
            sb.append(ZorkaUtil.urlEncode(e.getValue()));
        }

        this.uri = path + sb.toString();
    }


    @Override
    protected void process(List<String> msgs) {
        for (String msg : msgs) {
            try {
                HttpMessage req = HttpMessage.POST(uri, msg, headers);
                HttpMessage res = client.exec(req);

                // TODO what about 302 ?
                if (res.getStatus() >= 400) {
                    log.warn(url + ": error " + res.getStatus());
                    if (log.isDebugEnabled()) {
                        log.debug(url + ": request: '" + req.getBodyAsString() + "'");
                        log.debug(url + ": response: '" + res.getBodyAsString() + "'");
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("HTTP: " + url + " -> " + res.getStatus());
                }
            } catch (Exception e) {
                log.error("Error sending HTTP request", e);
            }
        }
    }
}
