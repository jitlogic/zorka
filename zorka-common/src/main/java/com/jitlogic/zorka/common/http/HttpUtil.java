/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUtil {

    private static HttpClient client = new MiniHttpClient();
    public static final Pattern RE_HTTP_URL = Pattern.compile("(https?)://([^/]+)(/.*)");

    public static void setHttpClient(HttpClient httpClient) {
        client = httpClient;
    }

    public static HttpRequest GET(String url) throws IOException {
        Matcher m = RE_HTTP_URL.matcher(url);
        if (m.matches()) {
            HttpRequest req = new HttpRequest(client, url);
            req.setMethod("GET");
            req.setHeader("Host", m.group(2));
            return req;
        } else {
            throw new ZorkaRuntimeException("Invalid URL: '" + url + "'");
        }
    }

    public static HttpRequest POST(String url, String body) throws IOException {
        Matcher m = RE_HTTP_URL.matcher(url);
        if (m.matches()) {
            HttpRequest req = new HttpRequest(client, url);
            req.setMethod("POST");
            req.setBody(body.getBytes());
            req.setHeader("Host", m.group(2));
            return req;
        } else {
            throw new ZorkaRuntimeException("Invalid URL: '" + url + "'");
        }
    }

    public static HttpRequest POST(String url, InputStream bodyStream, int bodyLength) throws IOException {
        Matcher m = RE_HTTP_URL.matcher(url);
        if (m.matches()) {
            HttpRequest req = new HttpRequest(client, url);
            req.setMethod("POST");
            req.setBody(bodyStream, bodyLength);
            req.setHeader("Host", m.group(2));
            return req;
        } else {
            throw new ZorkaRuntimeException("Invalid URL: '" + url + "'");
        }
    }

    public static HttpRequest POST(String url, byte [] body) throws IOException {
        Matcher m = RE_HTTP_URL.matcher(url);
        if (m.matches()) {
            HttpRequest req = new HttpRequest(client, url);
            req.setMethod("POST");
            req.setBody(body);
            req.setHeader("Host", m.group(2));
            return req;
        } else {
            throw new ZorkaRuntimeException("Invalid URL: '" + url + "'");
        }
    }


}
