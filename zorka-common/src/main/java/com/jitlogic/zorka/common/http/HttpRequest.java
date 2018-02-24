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

package com.jitlogic.zorka.common.http;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * This class represents an HTTP Request message.
 */
public class HttpRequest extends HttpMsg {

    public final static int F_CNT_LENGTH = 0x01;  // Send Content-Length header
    public final static int F_ENC_BASE64 = 0x02;  // Encode body using base64

    private Map<String, String> params = new HashMap<String, String>();

    private HttpClient client;

    boolean ignoreResp = false;

    String url;
    String method = "GET";

    int flags;


    public HttpRequest(HttpClient client, String url) throws IOException {
        this.client = client;
        this.url = url;
    }


    public void setParams(String...ps) {
        for (int i = 1; i < ps.length; i += 2) {
            params.put(ps[i-1], ps[i]);
        }
    }

    public String getParam(String name) {
        return params.get(name);
    }

    public void setParam(String name, String value) {
        params.put(name, value);
    }

    public Map<String,String> getParams() {
        return params;
    }

    public HttpRequest withHeader(String name, String value) {
        setHeader(name, value);
        return this;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isIgnoreResp() {
        return ignoreResp;
    }

    public void setIgnoreResp(boolean ignoreResp) {
        this.ignoreResp = ignoreResp;
    }

    public HttpResponse go() throws IOException {
        return client.execute(this);
    }
}
