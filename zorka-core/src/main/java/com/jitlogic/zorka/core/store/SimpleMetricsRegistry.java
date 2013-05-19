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

package com.jitlogic.zorka.core.store;

import com.jitlogic.zorka.core.perfmon.Metric;
import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.util.ByteBuffer;
import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleMetricsRegistry implements MetricsRegistry {

    private static final ZorkaLog log = ZorkaLogger.getLog(SimpleMetricsRegistry.class);

    private AtomicInteger lastTemplateId = new AtomicInteger(0);
    private Map<Integer,MetricTemplate> templateById = new ConcurrentHashMap<Integer, MetricTemplate>();
    private Map<MetricTemplate,MetricTemplate> templates = new ConcurrentHashMap<MetricTemplate, MetricTemplate>();

    private AtomicInteger lastMetricId = new AtomicInteger(0);
    private Map<Integer,Metric> metricById = new ConcurrentHashMap<Integer, Metric>();

    private ZorkaAsyncThread<byte[]> output;

    public SimpleMetricsRegistry(ZorkaAsyncThread<byte[]> output) {
        this.output = output;
    }


    @Override
    public void add(MetricTemplate template) {
        int id = template.getId();
        if (id != 0) {
            templateById.put(id, template);
            templates.put(template, template);
            if (id > lastTemplateId.get()) {
                lastTemplateId.set(id);
            }
        } else {
            getTemplate(template);
        }
    }


    @Override
    public MetricTemplate getTemplate(MetricTemplate template) {

        MetricTemplate mt = templates.get(template);
        if (mt != null) {
            return mt;
        }

        if (template.getId() == 0) {
            template.setId(lastTemplateId.incrementAndGet());
            templateById.put(template.getId(), template);
            templates.put(template, template);
            if (output != null) {
                ByteBuffer buf = new ByteBuffer(128);
                new SimplePerfDataFormat(buf).newMetricTemplate(template);
                while (!output.submit(buf.getContent()));
            }
        }
        return template;
    }


    @Override
    public MetricTemplate getTemplate(int id) {
        return templateById.get(id);
    }


    @Override
    public void add(Metric metric) {
        int id = metric.getId();
        if (id != 0) {
            metricById.put(id, metric);
            if (id > lastMetricId.get()) {
                lastMetricId.set(id);
            }
        }
    }


    @Override
    public Metric getMetric(Metric metric) {
        if (metric.getId() == 0) {
            metric.setId(lastMetricId.incrementAndGet());
            metricById.put(metric.getId(), metric);
            if (output != null) {
                ByteBuffer buf = new ByteBuffer(128);
                new SimplePerfDataFormat(buf).newMetric(metric);
                while (!output.submit(buf.getContent()));
            }
        }
        return metric;
    }


    @Override
    public Metric getMetric(int id) {
        return metricById.get(id);
    }


    @Override
    public int numMetrics() {
        return metricById.size();
    }


    @Override
    public int numTemplates() {
        return templateById.size();
    }
}
