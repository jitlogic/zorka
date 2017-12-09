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

public class GraphiteTextTracerOutput implements ZorkaSubmitter<SymbolicRecord> {

    private static Logger log = LoggerFactory.getLogger(GraphiteTextTracerOutput.class);

    private Map<String,String> constAttrMap;
    private Map<String,String> config;
    private PerfAttrFilter attrFilter;
    private PerfSampleFilter filter;
    private ZorkaSubmitter<String> output;
    private SymbolRegistry symbolRegistry;

    private int chunkSize = 4096;
    private String prefix = "zorka";

    public GraphiteTextTracerOutput(
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
        String pr = config.get("prefix");
        if (pr != null) {
            this.prefix = pr;
        }
        this.prefix += ".";
        this.symbolRegistry = symbolRegistry;
        this.config = config;
        this.constAttrMap = constAttrMap;
        this.attrFilter = attrFilter;
        this.filter = filter;
        this.output = output;
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        boolean rslt = true;

        if (sr instanceof PerfRecord) {
            PerfRecord pr = (PerfRecord)sr;
            if (log.isTraceEnabled()) {
                log.trace("Got data: " + sr);
            }
            StringBuilder sb = new StringBuilder(chunkSize);
            long t = System.currentTimeMillis()/1000;
            for (PerfSample ps : pr.getSamples()) {
                if (filter.matches(ps)) {
                    StringBuilder rec = new StringBuilder(256);
                    rec.append(prefix);
                    appendNormalized(rec, ps.getMetric().getName());
                    for (Map.Entry<String,String> e : constAttrMap.entrySet()) {
                        rec.append(';');
                        appendNormalized(rec, e.getKey());
                        rec.append('=');
                        appendNormalized(rec, e.getValue());
                    }
                    if (ps.getAttrs() != null) {
                        for (Map.Entry<Integer,String> e : ps.getAttrs().entrySet()) {
                            rec.append(';');
                            appendNormalized(rec, symbolRegistry.symbolName(e.getKey()));
                            rec.append('=');
                            appendNormalized(rec, e.getValue());
                        }
                    }
                    if (ps.getMetric().getAttrs() != null) {
                        for (Map.Entry<String,Object> e : ps.getMetric().getAttrs().entrySet()) {
                            rec.append(';');
                            appendNormalized(rec, e.getKey());
                            rec.append('=');
                            appendNormalized(rec, e.getValue().toString());
                        }
                    }
                    rec.append(' ');
                    rec.append(ps.getValue());
                    rec.append(' ');
                    rec.append(t);
                    rec.append('\n');

                    if (sb.length()+rec.length() > chunkSize) {
                        if (log.isTraceEnabled()) {
                            log.trace("Submit: '" + sb + "'");
                        }
                        rslt &= output.submit(sb.toString());
                        sb = new StringBuilder(chunkSize);
                    }
                    sb.append(rec);
                } // if (filter.matches(ps)
            } // for (PerSample ps : pr.getSamples())
            if (sb.length() > 2) {
                if (log.isTraceEnabled()) {
                    log.trace("Submit: '" + sb + "'");
                }
                rslt &= output.submit(sb.toString());
            }
        }

        return rslt;
    }

    private void appendNormalized(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
    }

}
