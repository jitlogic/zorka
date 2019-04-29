/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.core.test.support;

import com.jitlogic.zorka.common.ZorkaSubmitter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TestStringOutput implements ZorkaSubmitter<byte[]> {

    private List<String> results = new ArrayList<String>();

    public List<String> getResults() {
        return results;
    }

    @Override
    public boolean submit(byte[] item) {
        return results.add(new String(item, Charset.defaultCharset()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String r : results) {
            sb.append(r);
            sb.append('\n');
        }
        return sb.toString();
    }
}
