package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;

import java.util.Map;

public class OpenTsdbPushOutput extends AbstractMetricPushOutput {

    public OpenTsdbPushOutput(
            SymbolRegistry symbolRegistry,
            Map<String, String> config,
            Map<String, String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<String> output) {

        super(symbolRegistry, constAttrMap, attrFilter, filter, output);

        chunkStart = "[";
        chunkEnd = "]";
        chunkSep = ",";

        configure(config);
    }

    @Override
    protected void appendName(StringBuilder rec, String name) {
        rec.append("{\"metric\":\"");
        escape(rec, name);
        rec.append("\",");
    }

    @Override
    protected void appendAttr(StringBuilder rec, String key, String val, int num) {
        if (num == 0) {
            rec.append("\"tags\":{");
        } else {
            rec.append(',');
        }
        rec.append('"');
        escape(rec, key);
        rec.append("\":\"");
        escape(rec, val);
        rec.append("\"");
    }

    @Override
    protected void appendFinish(StringBuilder rec, Number val, long tstamp) {
        rec.append("},\"value\":");
        rec.append(val);
        rec.append(",\"timestamp\":");
        rec.append(tstamp);
        rec.append('}');
    }
}
