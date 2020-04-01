/*
 * Copyright (c) 2012-2020 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.PerfTextChunk;
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
            ZorkaSubmitter<PerfTextChunk> output) {

        super(symbolRegistry, constAttrMap, attrFilter, filter, output);

        sep = '_';

        configure(config);
    }

    @Override
    protected void appendDesc(StringBuilder rec, String name, String type, String desc) {
        if (name != null) {
            if (desc != null) {
                rec.append("# HELP "); rec.append(name); rec.append(' ');
                rec.append(desc); rec.append('\n');
            }
            if (type != null) {
                rec.append("# TYPE "); rec.append(name); rec.append(' ');
                rec.append(type); rec.append('\n');
            }
        }
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
