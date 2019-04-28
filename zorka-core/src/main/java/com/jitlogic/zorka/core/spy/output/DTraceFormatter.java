package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.util.List;

public interface DTraceFormatter {

    byte[] format(List<TraceRecord> acc, int softLimit, int hardLimit);


}
