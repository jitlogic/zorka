package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;

import java.util.Map;

public class GraphitePushOutput extends AbstractMetricPushOutput {

    public GraphitePushOutput(
            SymbolRegistry symbolRegistry,
            Map<String, String> config,
            Map<String, String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<String> output) {

        super(symbolRegistry, constAttrMap, attrFilter, filter, output);

        configure(config);
    }

    @Override
    protected void appendAttr(StringBuilder rec, String key, String val, int num) {
        rec.append(';');
        rec.append(normalize(key));
        rec.append('=');
        rec.append(normalize(val));
    }

    @Override
    protected void appendFinish(StringBuilder rec, Number val, long t) {
        rec.append(' ');
        rec.append(val);
        rec.append(' ');
        rec.append(t/1000);
    }
}
