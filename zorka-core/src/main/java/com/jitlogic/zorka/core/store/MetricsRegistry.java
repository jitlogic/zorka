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

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsRegistry {

    private AtomicInteger lastTemplateId;
    private NavigableMap<Integer,MetricTemplate> templateById;
    private Map<MetricTemplate,MetricTemplate> templates = new ConcurrentHashMap<MetricTemplate, MetricTemplate>();

    private AtomicInteger lastMetricId;
    private NavigableMap<Integer,Metric> metricById;


    public MetricsRegistry() {
        templateById = new ConcurrentSkipListMap<Integer, MetricTemplate>();
        metricById = new ConcurrentSkipListMap<Integer, Metric>();

        lastTemplateId = new AtomicInteger(0);
        lastMetricId = new AtomicInteger(0);
    }


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


    public MetricTemplate getTemplate(MetricTemplate template) {

        MetricTemplate mt = templates.get(template);
        if (mt != null) {
            return mt;
        }

        if (template.getId() == 0) {
            template.setId(lastTemplateId.incrementAndGet());
            templateById.put(template.getId(), template);
            templates.put(template, template);
        }
        return template;
    }


    public MetricTemplate getTemplate(int id) {
        return templateById.get(id);
    }


    public void add(Metric metric) {
        int id = metric.getId();
        if (id != 0) {
            metricById.put(id, metric);
            if (id > lastMetricId.get()) {
                lastMetricId.set(id);
            }
        }
    }


    public Metric getMetric(Metric metric) {
        if (metric.getId() == 0) {
            metric.setId(lastMetricId.incrementAndGet());
            metricById.put(metric.getId(), metric);
        }
        return metric;
    }


    public Metric getMetric(int id) {
        return metricById.get(id);
    }


    public int numMetrics() {
        return metricById.size();
    }


    public int numTemplates() {
        return templateById.size();
    }

}
