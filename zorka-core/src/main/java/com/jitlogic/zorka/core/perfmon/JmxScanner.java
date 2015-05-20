/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rlewczuk on 03.05.14.
 */
public class JmxScanner {
    /**
     * Logger
     */
    protected static final ZorkaLog log = ZorkaLogger.getLog(JmxScanner.class);

    /**
     * Symbol registry
     */
    protected SymbolRegistry symbols;
    protected MetricsRegistry metricsRegistry;
    protected MBeanServerRegistry mBeanServerRegistry;

    protected boolean attachResults;

    /**
     * Query listers representing queries supplied at scanner construction time
     */
    protected List<QueryLister> listers = new ArrayList<QueryLister>();


    public JmxScanner(MBeanServerRegistry mBeanServerRegistry, MetricsRegistry metricRegistry,
                      SymbolRegistry symbols, List<QueryLister> listers) {

        this.mBeanServerRegistry = mBeanServerRegistry;
        this.metricsRegistry = metricRegistry;
        this.symbols = symbols;
        this.listers = listers;
    }


    public JmxScanner(MBeanServerRegistry mBeanServerRegistry, MetricsRegistry metricRegistry,
                      SymbolRegistry symbols, QueryDef...qdefs) {

        this.mBeanServerRegistry = mBeanServerRegistry;
        this.metricsRegistry = metricRegistry;
        this.symbols = symbols;

        for (QueryDef qdef : qdefs) {
            this.listers.add(new QueryLister(mBeanServerRegistry, qdef));
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

            String description = ObjectInspector.substitute(template.getDescription(), attrs);

            switch (template.getType()) {
                case MetricTemplate.RAW_DATA:
                    metric = metricsRegistry.getMetric(new RawDataMetric(template, template.getName(), description, attrs));
                    break;
                case MetricTemplate.RAW_DELTA:
                    metric = metricsRegistry.getMetric(new RawDeltaMetric(template, template.getName(), description, attrs));
                    break;
                case MetricTemplate.TIMED_DELTA:
                    metric = metricsRegistry.getMetric(new TimedDeltaMetric(template, template.getName(), description, attrs));
                    break;
                case MetricTemplate.WINDOWED_RATE:
                    metric = metricsRegistry.getMetric(new WindowedRateMetric(template, template.getName(), description, attrs));
                    break;
                case MetricTemplate.UTILIZATION:
                    metric = metricsRegistry.getMetric(new UtilizationMetric(template, template.getName(), description, attrs));
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


    public List<PerfSample> getPerfSamples(long clock, QueryLister lister) {
        List<PerfSample> smpl = new ArrayList<PerfSample>();
        for (QueryResult result : lister.list()) {
            Metric metric = getMetric(lister.getMetricTemplate(), result);
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

            if (attachResults) {
                sample.setResult(result);
                sample.setMetric(metric);
            }

            // Add dynamic attributes if necessary
            if (metric.getDynamicAttrs() != null) {
                Map<Integer, String> attrs = new HashMap<Integer, String>();
                for (Map.Entry<String, Integer> e : metric.getDynamicAttrs().entrySet()) {
                    attrs.put(e.getValue(), result.getAttr(e.getKey()).toString());
                }
                sample.setAttrs(attrs);
            }

            log.trace(ZorkaLogger.ZPM_RUN_TRACE, "Submitting sample: %s", sample);
            smpl.add(sample);
        }
        return smpl;
    }


    public List<PerfSample> getPerfSamples(long clock) {
        List<PerfSample> samples = new ArrayList<PerfSample>();

        for (QueryLister lister : listers) {
            MetricTemplate template = lister.getMetricTemplate();
            if (template != null) {

                log.debug(ZorkaLogger.ZPM_RUN_DEBUG, "Scanning query: %s", lister);
                AgentDiagnostics.inc(AgentDiagnostics.PMON_QUERIES);

                samples.addAll(getPerfSamples(clock, lister));
            }
        }
        return samples;
    }

    public void setAttachResults(boolean attachResults) {
        this.attachResults = attachResults;
    }
}
