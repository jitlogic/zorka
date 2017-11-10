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

import java.io.IOException;

public class HttpUtil {

    private static HttpClient client = new MiniHttpClient();

    public static HttpRequest GET(String url) throws IOException {
        HttpRequest req = new HttpRequest(client, url);
        req.setMethod("GET");
        return req;
    }

    public static HttpRequest POST(String url, String body) throws IOException {
        HttpRequest req = new HttpRequest(client, url);
        req.setMethod("POST");
        req.setBody(body.getBytes());
        return req;
    }

    public static HttpRequest POST(String url, byte [] body) throws IOException {
        HttpRequest req = new HttpRequest(client, url);
        req.setMethod("POST");
        req.setBody(body);
        return req;
    }


}
