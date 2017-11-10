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
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;

import java.util.HashMap;
import java.util.Map;

public class InfluxTracerOutput implements ZorkaSubmitter<SymbolicRecord> {

    private Map<String,String> constTags = new HashMap<String, String>();
    private Map<String,String> dynamicTags = new HashMap<String, String>();

    private ZorkaSubmitter<String> output;

    public InfluxTracerOutput(Map<String,String> constTags, Map<String,String> dynamicTags, ZorkaSubmitter<String> output) {
        this.constTags = constTags;
        this.dynamicTags = dynamicTags;
        this.output = output;
    }

    private void normalize(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        if (sr instanceof PerfRecord) {
            PerfRecord pr = (PerfRecord)sr;
            for (PerfSample ps : pr.getSamples()) {
                StringBuilder sb = new StringBuilder(256);
                sb.append(ps.getMetric().getName());
                for (Map.Entry<String,String> e : constTags.entrySet()) {
                    sb.append(',');
                    sb.append(e.getKey());
                    sb.append('=');
                    normalize(sb, e.getValue());
                }
                for (Map.Entry<String,String> e : dynamicTags.entrySet()) {
                    sb.append(',');
                    sb.append(e.getKey());
                    sb.append('=');
                    normalize(sb, ps.getAttrs().get(ps.getMetric().getDynamicAttrs().get(e.getValue())));
                }
                sb.append(" value=");
                sb.append(ps.getValue().toString());
                sb.append(' ');
                sb.append(pr.getClock());
                output.submit(sb.toString());
            }
        }
        return false;
    }
}
