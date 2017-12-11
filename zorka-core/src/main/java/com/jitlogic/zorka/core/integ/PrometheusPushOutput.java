package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;

import java.util.Map;

public class PrometheusPushOutput extends AbstractMetricPushOutput {

    public PrometheusPushOutput(
            SymbolRegistry symbolRegistry,
            Map<String, String> config,
            Map<String, String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<String> output) {

        super(symbolRegistry, constAttrMap, attrFilter, filter, output);

        sep = '_';

        configure(config);
    }

    @Override
    protected void appendAttr(StringBuilder rec, String key, String val, int nattr) {
        rec.append(nattr == 0 ? '{' : ',');

        rec.append(normalize(key));
        rec.append("=\"");
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            rec.append(c == '"' ? '\'' : c);
        }
        rec.append('"');
    }

    @Override
    protected void appendFinish(StringBuilder rec, Number val, long tstamp) {
        rec.append("} ");
        rec.append(val);
    }
}
