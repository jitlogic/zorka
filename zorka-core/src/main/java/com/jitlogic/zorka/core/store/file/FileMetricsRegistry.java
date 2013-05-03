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

package com.jitlogic.zorka.core.store.file;

import com.jitlogic.zorka.core.perfmon.Metric;
import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.store.MetricsRegistry;
import com.jitlogic.zorka.core.store.SimplePerfDataFormat;
import com.jitlogic.zorka.core.util.ByteBuffer;
import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FileMetricsRegistry extends ZorkaAsyncThread<byte[]> implements MetricsRegistry {

    /** Output file path */
    private String path;

    /** Output file */
    private OutputStream output;

    private AtomicInteger lastTemplateId = new AtomicInteger(0);
    private Map<Integer,MetricTemplate> templateById = new ConcurrentHashMap<Integer, MetricTemplate>();

    private AtomicInteger lastMetricId = new AtomicInteger(0);
    private Map<Integer,Metric> metricById = new ConcurrentHashMap<Integer, Metric>();


    public FileMetricsRegistry(String path) {
        super("file-metrics-registry");
        this.path = path;
    }


    @Override
    public int templateId(MetricTemplate template) {

        if (template.getId() == 0) {
            template.setId(lastTemplateId.incrementAndGet());
            templateById.put(template.getId(), template);
            ByteBuffer buf = new ByteBuffer(128);
            new SimplePerfDataFormat(buf).newMetricTemplate(template);
            submit(buf.getContent());
        }

        return template.getId();
    }


    @Override
    public MetricTemplate getTemplate(int id) {
        return templateById.get(id);
    }


    @Override
    public int metricId(Metric metric) {
        if (metric.getId() == 0) {
            metric.setId(lastMetricId.incrementAndGet());
            metricById.put(metric.getId(), metric);
            ByteBuffer buf = new ByteBuffer(128);
            new SimplePerfDataFormat(buf).newMetric(metric);
            submit(buf.getContent());
        }
        return metric.getId();
    }


    @Override
    public Metric getMetric(int id) {
        return metricById.get(id);
    }


    @Override
    public int metricsCount() {
        return metricById.size();
    }


    @Override
    protected void process(byte[] chunk) {
        try {
            output.write(chunk);
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot write to " + path + ". Reopening file.", e);
            reopen();
            submit(chunk);
        } catch (NullPointerException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Attempt to write to closed file: " + path);
            reopen();
            submit(chunk);
        }
    }

    private void reopen() {
        log.info(ZorkaLogger.ZCL_CONFIG, "Reopening symbols file: " + path);
        close();
        open();
    }

    @Override
    protected synchronized void open() {
        if (output == null) {
            try {
                output = new FileOutputStream(path, true);
            } catch (FileNotFoundException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot open symbols file for write: " + path, e);
            }
        }
    }


    @Override
    protected synchronized void close() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot close symbols file: " + path, e);
            }
            output = null;
        }
    }


    @Override
    protected void flush() {
        if (output != null) {
            try {
                output.flush();
            } catch (IOException e) {
                log.warn(ZorkaLogger.ZCL_ERRORS, "Cannot flush symbols file: " + path, e);
            }
        }
    }

}
