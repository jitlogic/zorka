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
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsRegistry implements Closeable {

    private static final ZorkaLog log = ZorkaLogger.getLog(MetricsRegistry.class);

    private AtomicInteger lastTemplateId;
    private NavigableMap<Integer,MetricTemplate> templateById;
    private Map<MetricTemplate,MetricTemplate> templates = new ConcurrentHashMap<MetricTemplate, MetricTemplate>();

    private AtomicInteger lastMetricId;
    private NavigableMap<Integer,Metric> metricById;

    private DB db;


    public MetricsRegistry() {
        templateById = new ConcurrentSkipListMap<Integer, MetricTemplate>();
        metricById = new ConcurrentSkipListMap<Integer, Metric>();

        lastTemplateId = new AtomicInteger(0);
        lastMetricId = new AtomicInteger(0);
    }


    public MetricsRegistry(File file) {

        db = DBMaker.newFileDB(file)
                .randomAccessFileEnable()
                .closeOnJvmShutdown()
                .asyncFlushDelay(1)
                .make();

        templateById = db.getTreeMap("templates");
        lastTemplateId = new AtomicInteger(templateById.size() > 0 ? templateById.lastKey() : 0);

        for (Map.Entry<Integer,MetricTemplate> e : templateById.entrySet()) {
            templates.put(e.getValue(), e.getValue());
        }

        metricById = db.getTreeMap("metrics");
        lastMetricId = new AtomicInteger(metricById.size() > 0 ? metricById.lastKey() : 0);
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

    public void flush() {
        if (db != null) {
            db.commit();
        }
    }

    @Override
    public void close() throws IOException {
        if (db != null) {
            db.commit();
            db.close();
            db = null;
        }
    }
}
