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
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.perfmon.PerfAttrFilter;
import com.jitlogic.zorka.core.perfmon.PerfSampleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractMetricPushOutput implements ZorkaSubmitter<SymbolicRecord> {

    /** Logger is instantiated in order to indicate specific implementation. */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /** Constant attributes (always added to submitted metrics). */
    protected Map<String,String> constAttrMap;

    /** Attribute filter determines which attributes should be submitted. */
    private PerfAttrFilter attrFilter;

    /** Sample filter determines which metric samples should be submitted. */
    private PerfSampleFilter filter;

    /** Submitted responsible for sending data to monitoring system. */
    private ZorkaSubmitter<PerfTextChunk> output;

    /** Symbol registry. */
    private SymbolRegistry symbolRegistry;

    /** Maximum amount of data per request to be sent. */
    int chunkSize = 4096, chunkLead = 2, chunkTrail = 4;

    /** Prefix added to all metric names. */
    protected String prefix;

    /** Path separator character for metric names. */
    char sep = '.';

    /** Substitution character for string normalization. */
    String sub = "_";

    String chunkStart = "", chunkEnd = "\n", chunkSep = "\n";

    private boolean appendUnits = true;

    private boolean sortSamples = true;


    AbstractMetricPushOutput(
            SymbolRegistry symbolRegistry,
            Map<String,String> constAttrMap,
            PerfAttrFilter attrFilter,
            PerfSampleFilter filter,
            ZorkaSubmitter<PerfTextChunk> output) {
        this.symbolRegistry = symbolRegistry;
        this.constAttrMap = constAttrMap;
        this.attrFilter = attrFilter;
        this.filter = filter;
        this.output = output;
    }


    void configure(Map<String,String> config) {

        String ps = config.get("chunk.size");
        if (ps != null) {
            if (ps.matches("\\d+")) {
                chunkSize = Integer.parseInt(ps);
            } else {
                log.error("Invalid value of *.chunk.size: '{}' (should be integer)", ps);
            }
        }

        String pr = config.get("prefix");
        if (pr != null) {
            this.prefix = pr;
        } else {
            this.prefix = "";
        }

        if (this.prefix.length() > 0) {
            this.prefix += sep;
        }
    }


    protected void escape(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c == '"' ? '\'' : c);
        }
    }


    protected String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((Character.isJavaIdentifierPart(c)||c == sep)) {
                sb.append(c);
            } else {
                sb.append(sub);
            }
        }

        return sb.toString();
    }

    protected void appendName(StringBuilder rec, String name) {
        rec.append(normalize(prefix));
        rec.append(normalize(name));
    }

    protected void appendDesc(StringBuilder rec, String name, String type, String desc) { }

    protected abstract void appendAttr(StringBuilder rec, String key, String val, int num);

    protected abstract void appendFinish(StringBuilder rec, Number val, long tstamp);

    public final static String GAUGE = "gauge";
    public final static String COUNTER = "counter";
    public final static String SUMMARY_COUNT = "summary/count";
    public final static String SUMMARY_SUM = "summary/sum";

    private String substitute(PerfSample ps, String s) {
        if (ps.getMetric().getAttrs() != null && s != null && s.contains("${")) {
            return ObjectInspector.substitute(s, ZorkaUtil.<String,Object>map("ATTR", ps.getMetric().getAttrs()));
        } else {
            return s;
        }
    }

    public List<PerfSample> psort(List<PerfSample> samples) {
        if (!sortSamples) return samples;
        List<PerfSample> rslt = new ArrayList<PerfSample>(samples.size());
        Map<String,List<PerfSample>> sums = new HashMap<String, List<PerfSample>>();

        for (PerfSample ps : samples) {
            if (ps.getMetric() != null && ps.getMetric().getTemplate() != null) {
                Metric m = ps.getMetric();
                MetricTemplate mt = m.getTemplate();
                if (SUMMARY_COUNT.equals(mt.getType()) || SUMMARY_SUM.equals(mt.getType())) {
                    String key = m.getName() + m.getAttrs();
                    if (!sums.containsKey(key)) sums.put(key, new ArrayList<PerfSample>());
                    sums.get(key).add(ps);
                } else {
                    rslt.add(ps);
                }
            }
        }

        for (Map.Entry<String,List<PerfSample>> e : sums.entrySet()) {
            rslt.addAll(e.getValue());
        }

        return rslt;
    }

    @Override
    public boolean submit(SymbolicRecord sr) {
        boolean rslt = true;

        long t = System.currentTimeMillis();

        if (sr instanceof PerfRecord) {
            PerfRecord pr = (PerfRecord)sr;
            if (log.isTraceEnabled()) {
                log.trace("Received data: {}", pr);
            }

            StringBuilder sb = new StringBuilder(chunkSize);
            Set<String> labels = new TreeSet<String>();
            String chs = chunkStart;

            for (PerfSample ps : psort(pr.getSamples())) {
                Metric metric = ps.getMetric();
                if (metric == null) {
                    log.error("Cannot submit sample (metric description missing): {}", ps);
                    continue;
                }
                if (filter.matches(ps)) {
                    MetricTemplate mt = metric.getTemplate();
                    String name = metric.getName();
                    if (appendUnits) name += sep + mt.getUnits();

                    StringBuilder rec = new StringBuilder(256);

                    if (!labels.contains(name)) {
                        String type = mt.getType();
                        if (type.startsWith("summary/")) type = "summary";
                        appendDesc(rec, name, type, substitute(ps, mt.getDescription()));
                        labels.add(name);
                    }

                    if (SUMMARY_COUNT.equals(mt.getType())) name += sep + "count";
                    if (SUMMARY_SUM.equals(mt.getType())) name += sep + "sum";

                    appendName(rec, name);
                    int nattr = 0;

                    if (constAttrMap != null) {
                        for (Map.Entry<String, String> e : constAttrMap.entrySet()) {
                            if (attrFilter.matches(e.getKey())) {
                                appendAttr(rec, e.getKey(), e.getValue(), nattr); nattr++;
                            }
                        }
                    } // if (constAttrMap != null)

                    if (metric.getAttrs() != null) {
                        for (Map.Entry<String,Object> e : metric.getAttrs().entrySet()) {
                            if (attrFilter.matches(e.getKey()) && e.getValue() != null) {
                                appendAttr(rec, e.getKey(), e.getValue().toString(), nattr); nattr++;
                            }
                        }
                    } // if (ps.getMetric().getAttrs() != null)

                    appendFinish(rec, ps.getValue(), t);

                    if (sb.length()+rec.length() > chunkSize+chunkTrail) {
                        sb.append(chunkEnd);
                        if (log.isTraceEnabled()) {
                            log.trace("Submit: '{}'", sb);
                        }
                        rslt &= output.submit(new PerfTextChunk(pr.getLabel(), sb.toString().getBytes()));
                        sb = new StringBuilder(chunkSize);
                        chs = chunkStart;
                    }
                    sb.append(chs);
                    sb.append(rec);
                    chs = chunkSep;
                } // if (filter.matches(ps))
            } // for (PerfSample ps : pr.getSamples)
            if (sb.length() > chunkLead) {
                sb.append(chunkEnd);
                if (log.isTraceEnabled()) {
                    log.trace("Submit: '{}'", sb);
                }
                rslt &= output.submit(new PerfTextChunk(pr.getLabel(), sb.toString().getBytes()));
            }
        } // if (sr instanceof PerfRecord)

        return rslt;
    }
}
