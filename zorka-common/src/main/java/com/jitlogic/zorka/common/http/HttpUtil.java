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

import java.io.IOException;

public class HttpUtil {

    private static HttpClient client = new JdkHttpClient();

    public static HttpRequest GET(String url, String...params) throws IOException {
        return new HttpRequest(client, url).params(params).method("GET");
    }

    public static HttpRequest POST(String url, String body) throws IOException {
        return new HttpRequest(client, url).body(body).method("POST");
    }



}
