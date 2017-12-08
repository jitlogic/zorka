/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.PerfRecord;
import com.jitlogic.zorka.common.tracedata.PerfSample;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class InfluxTracerOutput implements ZorkaSubmitter<SymbolicRecord> {

    private static Logger log = LoggerFactory.getLogger(InfluxTracerOutput.class);

    // TODO wydzielić dedykowany filtr do atrybutów - osobna klasa używana też w innych outputach

    private Map<String,String> constAttrMap;
    private PerfAttrFilter attrFilter;
    private PerfSampleFilter filter;
    private ZorkaSubmitter<String> output;
    private SymbolRegistry symbolRegistry;

    public InfluxTracerOutput(
            SymbolRegistry symbolRegistry,
            Map<String,String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<String> output) {
        this.symbolRegistry = symbolRegistry;
        this.constAttrMap = constAttrMap;
        this.attrFilter = attrFilter;
        this.filter = filter;
        this.output = output;
    }

    private void appendNormalized(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        if (sr instanceof PerfRecord) {
            PerfRecord pr = (PerfRecord)sr;
            log.debug("Got data: " + sr);
            long t = System.currentTimeMillis();
            for (PerfSample ps : pr.getSamples()) {
                if (filter.matches(ps)) {
                    StringBuilder sb = new StringBuilder(256);
                    sb.append(ps.getMetric().getName());
                    for (Map.Entry<String, String> e : constAttrMap.entrySet()) {
                        sb.append(',');
                        sb.append(e.getKey());
                        sb.append('=');
                        appendNormalized(sb, e.getValue());
                    }
                    if (ps.getAttrs() != null) {
                        for (Map.Entry<Integer, String> e : ps.getAttrs().entrySet()) {
                            if (attrFilter.matches(e.getKey())) {
                                sb.append(',');
                                sb.append(symbolRegistry.symbolName(e.getKey()));
                                sb.append('=');
                                appendNormalized(sb, e.getValue());
                            }
                        }
                    }
                    if (ps.getMetric().getAttrs() != null) {
                        for (Map.Entry<String, Object> e : ps.getMetric().getAttrs().entrySet()) {
                            if (attrFilter.matches(e.getKey())) {
                                sb.append(',');
                                sb.append(e.getKey());
                                sb.append('=');
                                appendNormalized(sb, e.getValue().toString());
                            }
                        }
                    }
                    sb.append(" value=");
                    sb.append(ps.getValue().toString());
                    //sb.append(' ');
                    //sb.append(t);
                    String s = sb.toString();
                    log.debug("Submitting data: '" + s + "'");
                    output.submit(s);
                }
            }
        }
        return false;
    }
}
