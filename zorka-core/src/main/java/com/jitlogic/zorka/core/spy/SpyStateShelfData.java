/*
 * Copyright (c) 2012-2017 Rafał Lewczuk All Rights Reserved.
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
package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.LimitedTime;

import java.util.List;

public class SpyStateShelfData implements LimitedTime {

    private final long tstamp;
    private long timeout;
    private final List<Object> data;

    public SpyStateShelfData(long tstamp, long timeout, List<Object> data) {
        this.tstamp = tstamp;
        this.timeout = timeout;
        this.data = data;
    }

    public List<Object> getData() {
        return data;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeLimit() {
        return tstamp + timeout;
    }

    @Override
    public String toString() {
        return "SD(tst=" + tstamp + ", tmo=" + timeout + ", data=" + data + ")";
    }
}
