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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.lt.LTracer;

import java.util.Map;

public class TraceRecordFlagsProcessor implements SpyProcessor {

    private Tracer tracer;

    private int flags;

    public TraceRecordFlagsProcessor(Tracer tracer, int flags) {
        this.tracer = tracer;
        this.flags = flags;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        tracer.getHandler().markRecordFlags(flags);
        return record;
    }
}
