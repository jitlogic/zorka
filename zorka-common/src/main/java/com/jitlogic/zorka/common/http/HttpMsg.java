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

package com.jitlogic.zorka.common.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class HttpMsg {

    private Map<String, String> headers = new HashMap<String, String>();
    private byte[] body = new byte[0];

    public void setHeaders(String...hs) {
        for (int i = 1; i < hs.length; i += 2) {
            headers.put(hs[i-1], hs[i]);
        }
    }

    public Map<String,String> getHeaders() { return headers; }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public void setHeader(String name, String val) {
        headers.put(name, val);
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        try {
            return new String(body, "UTF-8"); // TODO variable encodings in the future
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


}
