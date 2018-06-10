package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;

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
