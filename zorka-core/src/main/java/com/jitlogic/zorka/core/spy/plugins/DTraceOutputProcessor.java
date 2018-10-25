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

import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TracerLib;

import java.util.Map;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

public class DTraceOutputProcessor implements SpyProcessor {

    private boolean nextTid;
    private boolean setAttrs;

    private TracerLib tracer;
    private ThreadLocal<DTraceState> dtraceLocal;

    public DTraceOutputProcessor(TracerLib tracer, ThreadLocal<DTraceState> dtraceLocal) {
        this(tracer, dtraceLocal, true, true);
    }

    public DTraceOutputProcessor(TracerLib tracer, ThreadLocal<DTraceState> dtraceLocal,
                                 boolean nextTid, boolean setAttrs) {
        this.tracer = tracer;
        this.dtraceLocal = dtraceLocal;
        this.nextTid = nextTid;
        this.setAttrs = setAttrs;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        DTraceState ds = dtraceLocal.get();

        if (ds != null) {
            String uuid = ds.getUuid();
            String tid = nextTid ? ds.nextTid() : ds.lastTid();

            rec.put(DTRACE_STATE, ds);
            rec.put(DTRACE_UUID, uuid);
            rec.put(DTRACE_OUT, tid);

            Long t1 = ((Long)rec.get("T1"));

            if (ds.getThreshold() >= 0 && t1 != null) {
                rec.put(DTRACE_XTT, Math.max(0, ds.getThreshold() - ((Long)rec.get("T1")-ds.getTstart()) / 1000000L));
            }

            if (setAttrs) {
                tracer.newAttr(DTRACE_UUID, uuid);
                tracer.newAttr(DTRACE_OUT, uuid + tid);
            }
        }

        return rec;
    }
}
