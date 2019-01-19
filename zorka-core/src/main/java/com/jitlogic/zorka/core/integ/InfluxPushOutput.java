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

package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;

import java.util.Map;

public class InfluxPushOutput extends AbstractMetricPushOutput {

    public InfluxPushOutput(
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
        rec.append(',');
        rec.append(normalize(key));
        rec.append('=');
        rec.append(normalize(val));
    }

    @Override
    protected void appendFinish(StringBuilder rec, Number val, long tstamp) {
        rec.append(" value=");
        rec.append(val);
        rec.append(' ');
        rec.append(tstamp);
    }
}
