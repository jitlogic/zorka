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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.tracedata.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.TracerLib;

import java.util.Map;
import java.util.Random;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

public class DTraceOutputProcessor implements SpyProcessor {

    private Tracer tracer;
    private TracerLib tracerLib;
    private ThreadLocal<DTraceState> dtraceLocal;
    private Random rand = new Random();

    private int delFlags;
    private int addFlags;

    public DTraceOutputProcessor(Tracer tracer, TracerLib tracerLib, ThreadLocal<DTraceState> dtraceLocal, int delFlags, int addFlags) {
        this.tracer = tracer;
        this.tracerLib = tracerLib;
        this.dtraceLocal = dtraceLocal;
        this.delFlags = delFlags;
        this.addFlags = addFlags;
    }

    private void formatZipkinCtx(DTraceState ds, Map<String,Object> rec, long spanId) {
        rec.put(DH_B3_TRACEID, ds.getTraceIdHex());
        rec.put(DH_B3_SPANID, ZorkaUtil.hex(spanId));
        rec.put(DH_B3_PARENTID, ds.getSpanIdHex());
        int flags = ds.getFlags();
        if (0 != (flags & F_SAMPLE)) rec.put(DH_B3_SAMPLED, "1");
        if (0 != (flags & F_DROP)) rec.put(DH_B3_SAMPLED, "0");
        if (0 != (flags & F_DEBUG)) rec.put(DH_B3_FLAGS, "1");
    }

    private void formatZipkinB3Ctx(DTraceState ds, Map<String,Object> rec, long spanId) {
        int flags = ds.getFlags();
        if (0 != (flags & F_DROP)) {
            rec.put(DH_B3, "0");
        } else {
            String f = (0 != (flags & F_SAMPLE)) ? "1" : (0 != (flags & F_DEBUG)) ? "d" : "0";
            rec.put(DH_B3, ds.getTraceIdHex() + "-" + ds.getSpanIdHex() + "-" + f + "-" + ZorkaUtil.hex(spanId));
        }
    }

    private void formatJaegerCtx(DTraceState ds, Map<String,Object> rec, long spanId) {
        rec.put(DH_UBER_TID, ds.getTraceIdHex() + ":" + ZorkaUtil.hex(spanId) + ":" + ds.getParentId() + ":" +
                String.format("%02x", (ds.getFlags() & 0xff) | (0 != (ds.getFlags() & F_DEBUG) ? 0x02 : 0x00)));
        for (Map.Entry<String,String> e : ds.getBaggage().entrySet()) {
            rec.put(DH_UBER_CTX + e.getKey(), e.getValue());
        }
    }

    private void formatW3Ctx(DTraceState ds, Map<String,Object> rec, long spanId) {
        String s = "00-" + ds.getTraceIdHex() + "-" + ds.getSpanIdHex() + "-" + String.format("%02x", ds.getFlags() & 0xff);
        rec.put(DH_W3_TRACEPARENT, s);
        if (ds.getTraceState() != null) rec.put(DH_W3_TRACESTATE,  ds.getTraceState());
    }


    @Override
    public Map<String, Object> process(Map<String, Object> rec) {
        DTraceState ds = dtraceLocal.get();

        if (ds != null) {
            ds = new DTraceState(ds);
            long spanId = rand.nextLong();
            int flags = (ds.getFlags() & ~delFlags) | addFlags;
            rec.put(DTRACE_STATE, ds);
            tracer.getHandler().setDTraceState(ds);
            String tid = ds.getTraceIdHex();
            rec.put(DT_TRACE_ID, tid);
            tracerLib.newAttr(DT_TRACE_ID, tid);
            String sid = ZorkaUtil.hex(spanId);
            rec.put(DT_SPAN_ID, sid);
            tracerLib.newAttr(DT_SPAN_ID, sid);
            long pid = ds.getSpanId();
            rec.put(DT_PARENT_ID, pid);
            tracerLib.newAttr(DT_PARENT_ID, pid);
            switch (ds.getFlags() & F_MODE_MASK) {
                case F_ZIPKIN_MODE: {
                    if (0 != (flags & F_B3_HDR)) {
                        formatZipkinB3Ctx(ds, rec, spanId);
                    } else {
                        formatZipkinCtx(ds, rec, spanId);
                    }

                    break;
                }
                case F_JAEGER_MODE: formatJaegerCtx(ds, rec, spanId); break;
                case F_W3_TRC_MODE: formatW3Ctx(ds, rec, spanId); break;
            }

            // TODO tutaj odesłanie kontekstu do właściwego typu kolektora (zipkin itd.).

            // TODO przemyśleć integrację atrybutów opentracing i standardowych atrybutów tracera.
        }

        return rec;
    }
}
