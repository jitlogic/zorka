/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


/**
 * This class represents an HTTP Request message.
 */
public class HttpRequest extends HttpMessage<HttpRequest> {

    Map<String, String> params = new HashMap<String, String>();
    Map<String, String> headers = new HashMap<String, String>();

    HttpClient client;
    String url;
    String method = "GET";

    public HttpRequest(HttpClient client, String url) throws IOException {
        this.client = client;
        this.url = url;
    }


    public HttpRequest params(String...ps) {
        for (int i = 1; i < ps.length; i += 2) {
            params.put(ps[i-1], ps[i]);
        }
        return this;
    }

    public String param(String name) {
        return params.get(name);
    }

    public Map<String,String> params() {
        return params;
    }

    public HttpRequest headers(String...hs) {
        for (int i = 1; i < hs.length; i += 2) {
            headers.put(hs[i-1], hs[i]);
        }
        return this;
    }

    public Map<String,String> headers() { return headers; }

    public String header(String name) {
        return headers.get(name);
    }

    public HttpRequest method(String method) {
        this.method = method;
        return this;
    }

    public String method() {
        return method;
    }

    /**
     * Returns request URL
     */
    public String url() {
        return url;
    }


    /**
     * Executes request. Returns reply object containing reply data.
     *
     * @return HttpResponse object
     */
    public HttpResponse go() throws IOException {
        return client.execute(this);
    }
}
