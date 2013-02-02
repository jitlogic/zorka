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

package com.jitlogic.zorka.agent.rankproc;

import com.jitlogic.zorka.agent.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.common.*;

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

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(JmxAttrScanner.class);

    /** Scanner ID (attached to every packet of sample data) */
    private int id;

    /** Symbol registry */
    private SymbolRegistry symbols;

    private MetricsRegistry metricsRegistry;

    /** Output handler - handles generated data (eg. saves them to trace files). */
    private PerfDataEventHandler output;

    /** Query listers representing queries supplied at scanner construction time */
    private List<QueryLister> listers = new ArrayList<QueryLister>();


    /**
     * Creates new JMX attribute scanner object.
     *
     * @param symbols symbol registry
     *
     * @param name scanner name (converted to ID using symbol registry and attached to every emitted packet of data).
     *@param registry MBean server registry object
     *@param output tracer output
     *@param qdefs JMX queries
     */
    public JmxAttrScanner(SymbolRegistry symbols, MetricsRegistry metricRegistry, String name,
                          MBeanServerRegistry registry, PerfDataEventHandler output, QueryDef... qdefs) {
        this.symbols = symbols;
        this.metricsRegistry = metricRegistry;
        this.id = symbols.symbolId(name);
        this.output = output;

        for (QueryDef qdef : qdefs) {
            this.listers.add(new QueryLister(registry, qdef));
        }
    }


    public Metric getMetric(MetricTemplate template, QueryResult result) {
        String key = result.getKey(template.getDynamicAttrs());

        Metric metric = template.getMetric(key);
        if (metric == null) {
            switch (template.getType()) {
                case MetricTemplate.RAW_DATA:
                    metric = new RawDataMetric(template, result.attrSet());
                    break;
                case MetricTemplate.RAW_DELTA:
                    metric = new RawDeltaMetric(template, result.attrSet());
                    break;
                case MetricTemplate.TIMED_DELTA:
                    metric = new TimedDeltaMetric(template, result.attrSet());
                    break;
                case MetricTemplate.WINDOWED_RATE:
                    metric = new WindowedRateMetric(template, result.attrSet());
                    break;
                default:
                    return null;
            }
            template.putMetric(key, metric);
            metricsRegistry.metricId(metric);

            if (template.getDynamicAttrs().size() > 0) {
                Map<String,Integer> dynamicAttrs = new HashMap<String,Integer>();
                for (String attr : template.getDynamicAttrs()) {
                    dynamicAttrs.put(attr, symbols.symbolId(attr));
                }
                metric.setDynamicAttrs(dynamicAttrs);
            }
        }

        return metric;
    }



    @Override
    public void run() {
        runCycle(System.currentTimeMillis());
    }


    /**
     * Performs one scan-submit cycle.
     *
     * @param clock current time (milliseconds since Epoch)
     */
    public void runCycle(long clock) {
        List<PerfSample> samples = new ArrayList<PerfSample>();

        for (QueryLister lister : listers) {
            MetricTemplate template = lister.getMetricTemplate();
            if (template != null) {
                for (QueryResult result : lister.list()) {
                    if (result.getValue() instanceof Number) {
                        Metric metric = getMetric(template, result);
                        Number val = metric.getValue(clock, (Number)result.getValue());
                        PerfSample sample = new PerfSample(metric.getId(),
                            val instanceof Double || val instanceof Float ? val.doubleValue() : val.longValue());
                        // Add dynamic attributes if necessary
                        if (metric.getDynamicAttrs() != null) {
                            Map<Integer,String> attrs = new HashMap<Integer, String>();
                            for (Map.Entry<String,Integer> e : metric.getDynamicAttrs().entrySet()) {
                                attrs.put(e.getValue(), result.getAttr(e.getKey()).toString());
                            }
                            sample.setAttrs(attrs);
                        }
                        samples.add(sample);
                    } else {
                        // TODO Log only when logging of this particular message is enabled
                        log.warn("Trying to submit non-numeric metric value for " + result.getAttrPath() + ": " + result.getValue());
                    }
                }
            }
        }

        if (samples.size() > 0) {
            output.perfData(clock, id, samples);
        }
    }

}
