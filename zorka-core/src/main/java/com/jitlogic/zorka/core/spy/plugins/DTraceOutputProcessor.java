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
import com.jitlogic.zorka.core.spy.TracerLib;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

public class DTraceOutputProcessor implements SpyProcessor {

    private TracerLib tracer;
    private AtomicLong dtraceTidGen;
    private ThreadLocal<String> uuidLocal;
    private ThreadLocal<String> tidLocal;

    public DTraceOutputProcessor(TracerLib tracer, AtomicLong dtraceTidGen, ThreadLocal<String> uuidLocal, ThreadLocal<String> tidLocal) {
        this.tracer = tracer;
        this.dtraceTidGen = dtraceTidGen;
        this.uuidLocal = uuidLocal;
        this.tidLocal = tidLocal;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        String uuid = uuidLocal.get();
        String tid = tidLocal.get();

        if (uuid != null && tid != null) {
            rec.put(DTRACE_UUID, uuid);
            String tid1 = String.format("%s%s%x", tid, DTRACE_SEP, dtraceTidGen.incrementAndGet());
            rec.put(DTRACE_OUT, tid1);
            tracer.newAttr(DTRACE_UUID, uuid);
            tracer.newAttr(DTRACE_OUT, uuid + tid1);
        }

        return rec;
    }
}
