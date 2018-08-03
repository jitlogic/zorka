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
