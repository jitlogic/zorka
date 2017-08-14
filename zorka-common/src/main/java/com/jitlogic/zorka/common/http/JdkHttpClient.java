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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class JdkHttpClient implements HttpClient {

    public HttpResponse execute(HttpRequest req) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(req.url());

        if (!req.params.isEmpty()) {
            sb.append('?');
            for (Map.Entry<String, String> e : req.params.entrySet()) {
                sb.append(e.getKey());
                sb.append('=');
                sb.append(e.getValue());
                sb.append('&');
            }
            sb.deleteCharAt(sb.length()-1);
        }

        URL url = new URL(sb.toString());

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(req.method);

        for (Map.Entry<String,String> e : req.headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }

        conn.setDoOutput(true);

        if (req.body != null) {
            conn.getOutputStream().write(req.body.getBytes());
            conn.getOutputStream().close();
        }

        BufferedReader rdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        sb = new StringBuilder();
        String line;

        while ((line = rdr.readLine()) != null) {
            sb.append(line);
        }
        rdr.close();

        return new HttpResponse()
            .setResponseCode(conn.getResponseCode())
            .setResponseMessage(conn.getResponseMessage())
            .headers(conn.getHeaderFields())
            .body(sb.toString());
    }
}
