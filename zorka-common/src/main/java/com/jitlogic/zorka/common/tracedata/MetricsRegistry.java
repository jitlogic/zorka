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

package com.jitlogic.zorka.common.tracedata;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps track of concrete instance of tracked performance metrics.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MetricsRegistry {

    /**
     * Last template ID
     */
    private AtomicInteger lastTemplateId;

    /**
     * Tracked templates (by ID)
     */
    private ConcurrentMap<Integer, MetricTemplate> templateById;

    /**
     * Tracked templates (by template)
     */
    private ConcurrentMap<MetricTemplate, MetricTemplate> templates = new ConcurrentHashMap<MetricTemplate, MetricTemplate>();

    /**
     * Last metric ID
     */
    private AtomicInteger lastMetricId;

    /**
     * Tracked metrics (by ID)
     */
    private ConcurrentMap<Integer, Metric> metricById;


    public MetricsRegistry() {
        templateById = new ConcurrentHashMap<Integer, MetricTemplate>();
        metricById = new ConcurrentHashMap<Integer, Metric>();

        lastTemplateId = new AtomicInteger(0);
        lastMetricId = new AtomicInteger(0);
    }


    /**
     * Adds new metric template
     *
     * @param template
     */
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


    /**
     * Checks if identical template is already registered. If so, returns it.
     * If not, assigns ID to template passed as argument, registers and returns it.
     *
     * @param template template to be verified
     * @return registered template
     */
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


    /**
     * Registers new metric.
     *
     * @param metric metric to be registered
     */
    public void add(Metric metric) {
        int id = metric.getId();
        if (id != 0) {
            metricById.put(id, metric);
            if (id > lastMetricId.get()) {
                lastMetricId.set(id);
            }
        }
    }


    /**
     * Checks if this metric has been registered. Registers when needed and returns registered metric.
     *
     * @param metric metric to be checked
     * @return registered metric
     */
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
