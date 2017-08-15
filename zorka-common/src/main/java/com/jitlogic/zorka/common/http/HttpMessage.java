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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Generic HTTP message class.
 */
public abstract class HttpMessage<T extends HttpMessage<T>> {

    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    String body;

    /**
     * Returns the Message body.
     *
     * @return message body
     */
    public String body() {
        return body;
    }

    /**
     * Sets message body.
     *
     * @param body  Body string.
     */
    @SuppressWarnings("unchecked")
    public T body(String body) {
        this.body = body;
        return (T) this;
    }

    public String getBody() {
        return body;
    }

    /**
     * Adds headers.
     */
    @SuppressWarnings("unchecked")
    public T headers(String...hdrs) {
        for (int i = 1; i < hdrs.length; i += 2) {
            String k = hdrs[i-1], v = hdrs[i];
            List<String> vals = headers.get(k);
            if (vals == null) {
                vals = new ArrayList<String>();
                headers.put(k, vals);
            }
            vals.add(v);
        }
        return (T) this;
    }



    @SuppressWarnings("unchecked")
    public T headers(Map<String,List<String>> headers) {
        this.headers = headers;
        return (T) this;
    }
}
