/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.spy.TracerOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JMX Attribute Scanner is responsible for traversing JMX (using supplied queries) and
 * submitting obtained metric data to tracer output. Results will be sorted into categories
 * (integers, long integers, double precision FP), attribute sets will be concatenated and
 * converted to symbols, data of each category will be submitted.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class JmxAttrScanner implements Runnable {

    /**
     * Logger
     */
    private static final ZorkaLog log = ZorkaLogger.getLog(JmxAttrScanner.class);

    /**
     * Scanner ID (attached to every packet of sample data)
     */
    private int id;
    private String name;

    /**
     * Symbol registry
     */
    private SymbolRegistry symbols;

    private MetricsRegistry metricsRegistry;

    /**
     * Output handler - handles generated data (eg. saves them to trace files).
     */
    private TracerOutput output;

    /**
     * Query listers representing queries supplied at scanner construction time
     */
    private List<QueryLister> listers = new ArrayList<QueryLister>();


    /**
     * Creates new JMX attribute scanner object.
     *
     * @param symbols  symbol registry
     * @param name     scanner name (converted to ID using symbol registry and attached to every emitted packet of data).
     * @param registry MBean server registry object
     * @param output   tracer output
     * @param qdefs    JMX queries
     */
    public JmxAttrScanner(SymbolRegistry symbols, MetricsRegistry metricRegistry, String name,
                          MBeanServerRegistry registry, TracerOutput output, QueryDef... qdefs) {
        this.symbols = symbols;
        this.metricsRegistry = metricRegistry;
        this.id = symbols.symbolId(name);
        this.name = name;
        this.output = output;

        for (QueryDef qdef : qdefs) {
            this.listers.add(new QueryLister(registry, qdef));
        }
    }


    public Metric getMetric(MetricTemplate template, QueryResult result) {
        String key = result.getKey(template.getDynamicAttrs());

        Metric metric = template.getMetric(key);
        if (metric == null) {
            Map<String, Object> attrs = new HashMap<String, Object>();

            for (Map.Entry<String, Object> e : result.attrSet()) {
                attrs.put(e.getKey(), e.getValue().toString());
            }

            String name = ObjectInspector.substitute(template.getName(), attrs);

            switch (template.getType()) {
                case MetricTemplate.RAW_DATA:
                    metric = metricsRegistry.getMetric(new RawDataMetric(template, name, attrs));
                    break;
                case MetricTemplate.RAW_DELTA:
                    metric = metricsRegistry.getMetric(new RawDeltaMetric(template, name, attrs));
                    break;
                case MetricTemplate.TIMED_DELTA:
                    metric = metricsRegistry.getMetric(new TimedDeltaMetric(template, name, attrs));
                    break;
                case MetricTemplate.WINDOWED_RATE:
                    metric = metricsRegistry.getMetric(new WindowedRateMetric(template, name, attrs));
                    break;
                case MetricTemplate.UTILIZATION:
                    metric = metricsRegistry.getMetric(new UtilizationMetric(template, name, attrs));
                    break;
                default:
                    return null;
            }

            if (template.getId() == 0) {
                metricsRegistry.getTemplate(template);
            }

            template.putMetric(key, metric);
            metricsRegistry.getMetric(metric);

            if (template.getDynamicAttrs().size() > 0) {
                Map<String, Integer> dynamicAttrs = new HashMap<String, Integer>();
                for (String attr : template.getDynamicAttrs()) {
                    dynamicAttrs.put(attr, symbols.symbolId(attr));
                }
                metric.setDynamicAttrs(dynamicAttrs);
            }

            log.debug(ZorkaLogger.ZPM_RUN_DEBUG, "Created new metric: " + metric);
            AgentDiagnostics.inc(AgentDiagnostics.METRICS_CREATED);
        }

        return metric;
    }


    @Override
    public void run() {
        try {
            runCycle(System.currentTimeMillis());
        } catch (Error e) {
            log.error(ZorkaLogger.ZPM_ERRORS, "Error executing scanner '" + name + "'", e);
            AgentDiagnostics.inc(AgentDiagnostics.PMON_ERRORS);
        }
    }


    /**
     * Performs one scan-submit cycle.
     *
     * @param clock current time (milliseconds since Epoch)
     */
    public void runCycle(long clock) {
        List<PerfSample> samples = new ArrayList<PerfSample>();

        AgentDiagnostics.inc(AgentDiagnostics.PMON_CYCLES);

        long t1 = System.nanoTime();

        for (QueryLister lister : listers) {
            MetricTemplate template = lister.getMetricTemplate();
            if (template != null) {

                log.debug(ZorkaLogger.ZPM_RUN_DEBUG, "Scanning query: %s", lister);
                AgentDiagnostics.inc(AgentDiagnostics.PMON_QUERIES);

                for (QueryResult result : lister.list()) {
                    Metric metric = getMetric(template, result);
                    Number val = metric.getValue(clock, result.getValue());

                    if (val == null) {
                        log.debug(ZorkaLogger.ZPM_RUN_DEBUG, "Obtained null value for metric '%s'. Skipping. ", metric);
                        AgentDiagnostics.inc(AgentDiagnostics.PMON_NULLS);
                        continue;
                    }

                    if (val instanceof Double || val instanceof Float) {
                        val = val.doubleValue();
                    } else {
                        val = val.longValue();
                    }

                    PerfSample sample = new PerfSample(metric.getId(), val);

                    // Add dynamic attributes if necessary
                    if (metric.getDynamicAttrs() != null) {
                        Map<Integer, String> attrs = new HashMap<Integer, String>();
                        for (Map.Entry<String, Integer> e : metric.getDynamicAttrs().entrySet()) {
                            attrs.put(e.getValue(), result.getAttr(e.getKey()).toString());
                        }
                        sample.setAttrs(attrs);
                    }

                    log.trace(ZorkaLogger.ZPM_RUN_TRACE, "Submitting sample: %s", sample);
                    samples.add(sample);
                }
            }
        }

        long t2 = System.nanoTime();

        log.info(ZorkaLogger.ZPM_RUNS, "Scanner %s execution took " + (t2 - t1) / 1000000L + " milliseconds to execute. "
                + "Collected samples: " + samples.size());

        AgentDiagnostics.inc(AgentDiagnostics.PMON_TIME, t2 - t1);
        AgentDiagnostics.inc(AgentDiagnostics.PMON_PACKETS_SENT);
        AgentDiagnostics.inc(AgentDiagnostics.PMON_SAMPLES_SENT, samples.size());

        if (samples.size() > 0) {
            output.submit(new PerfRecord(clock, id, samples));
        }
    }

}
