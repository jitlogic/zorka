/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.PerfRecord;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class OpenTsdbTracerOutput implements ZorkaSubmitter<SymbolicRecord> {

    private static Logger log = LoggerFactory.getLogger(OpenTsdbTracerOutput.class);

    private Map<String,String> constAttrMap;
    private PerfAttrFilter attrFilter;
    private PerfSampleFilter filter;
    private ZorkaSubmitter<String> output;
    private SymbolRegistry symbolRegistry;

    /** Limits single data packet size in bytes (this isn't strict value - will be probably exceeded up to about one kB). */
    private int chunkSize = 3072;

    public OpenTsdbTracerOutput(
            SymbolRegistry symbolRegistry,
            Map<String,String> config,
            Map<String,String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<String> output) {
        String ps = config.get("chunk.size");
        if (ps != null) {
            if (ps.matches("\\d+")) {
                chunkSize = Integer.parseInt(ps);
            } else {
                log.error("Invalid value of opentsdb.chunk.size: '" + ps + "' (should be integer)");
            }
        }
        this.symbolRegistry = symbolRegistry;
        this.constAttrMap = constAttrMap;
        this.attrFilter = attrFilter;
        this.filter = filter;
        this.output = output;
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        if (sr instanceof PerfRecord) {
            PerfRecord pr = (PerfRecord)sr;
            if (log.isTraceEnabled()) {
                log.trace("Got data: " + sr);
            }
            StringBuilder sb = new StringBuilder(1024);
            long t = System.currentTimeMillis();
            for (PerfSample ps : pr.getSamples()) {
                if (filter.matches(ps)) {
                    Map<String, Object> ma = new HashMap<String, Object>(constAttrMap);
                    if (ps.getAttrs() != null) {
                        for (Map.Entry<Integer, String> e : ps.getAttrs().entrySet()) {
                            if (attrFilter.matches(e.getKey())) {
                                ma.put(symbolRegistry.symbolName(e.getKey()), e.getValue());
                            }
                        }
                    }
                    if (ps.getMetric().getAttrs() != null) {
                        for (Map.Entry<String, Object> e : ps.getMetric().getAttrs().entrySet()) {
                            if (attrFilter.matches(e.getKey())) {
                                ma.put(e.getKey(), e.getValue().toString());
                            }
                        }
                    }
                    Map<String, Object> m = ZorkaUtil.map("tags", ma,
                            "metric", ps.getMetric().getName(), "timestamp", t, "value", ps.getValue());

                    String rec = new JSONWriter().write(m);
                    if (sb.length()+rec.length() > chunkSize-2) {
                        sb.append(']');
                        if (log.isTraceEnabled()) {
                            log.trace("Submit: " + sb);
                        }
                        output.submit(sb.toString());
                        sb = new StringBuilder(chunkSize);
                    }
                    sb.append(sb.length() == 0 ? '[' : ',');
                    sb.append(rec);
                }
            }

            sb.append(']');
            if (sb.length() > 2) {
                if (log.isTraceEnabled()) {
                    log.trace("Submit: " + sb);
                }
                output.submit(sb.toString());
            }
        }

        return true;
    }
}
