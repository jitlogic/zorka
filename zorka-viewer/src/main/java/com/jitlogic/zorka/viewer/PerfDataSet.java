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

package com.jitlogic.zorka.viewer;

import com.jitlogic.zorka.core.store.*;
import com.jitlogic.zorka.core.perfmon.PerfDataEventHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents set of traces read from single trace file.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class PerfDataSet implements PerfDataEventHandler {

    /** Symbol map */
    private Map<Integer,String> symbols = new HashMap<Integer, String>(4096);

    /** List of root records of all traces */
    private List<NamedTraceRecord> traces = new ArrayList<NamedTraceRecord>();

    /** Top of record stack (used by loading process, obsolete after load process ends) */
    private NamedTraceRecord top = new NamedTraceRecord(null);

    /** Collected performance metric data (indexed by metric ID). */
    private Map<Integer,PerfMetricData>  metricData = new HashMap<Integer, PerfMetricData>();

    private Map<Integer,MetricTemplate> metricTemplates = new HashMap<Integer, MetricTemplate>();

    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        top.setTraceName(symbols.get(traceId));
        top.setClock(clock);
        top.setTraceFlags(flags);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

        if (top.getClassName() != null) {
            top = new NamedTraceRecord(top);
        }

        top.setClassName(symbols.get(classId));
        top.setMethodName(symbols.get(methodId));
        top.setMethodSignature(symbols.get(signatureId));
        top.setTime(tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        top.setTime(tstamp-top.getTime());
        pop();
    }


    @Override
    public void traceError(Object exception, long tstamp) {
        top.setException((SymbolicException) exception);
        top.setTime(tstamp - top.getTime());
        pop();
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        top.setCalls(calls);
        top.setErrors(errors);
        top.setFlags(flags);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        symbols.put(symbolId, symbolText);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        top.setAttr(symbols.get(attrId), attrVal);
    }

    @Override
    public void disable() {
    }

    @Override
    public void enable() {
    }


    @Override
    public void perfData(long clock, int scannerId, List<PerfSample> samples) {
        for (PerfSample sample : samples) {
            PerfMetricData pmd = metricData.get(sample.getMetricId());
            pmd.setScannerId(scannerId);
            if (sample.getValue() instanceof Long) {
                pmd.addL(clock, sample.getValue().longValue());
            } else if (sample.getValue() instanceof Double) {
                pmd.addD(clock, sample.getValue().doubleValue());
            }
        }
    }

    @Override
    public void newMetricTemplate(MetricTemplate template) {
        metricTemplates.put(template.getId(), template);
    }

    @Override
    public void newMetric(Metric metric) {
        metric.setTemplate(metricTemplates.get(metric.getTemplateId()));
        PerfMetricData pmd = new PerfMetricData(metric);
        metricData.put(metric.getId(), pmd);
        //metrics.put(metric.getId(), metric);
        // TODO add metric object to metric template hash map
    }


    private List<PerfSample> getSamples(int metricId, Map<Integer,List<PerfSample>> mdata) {
        List<PerfSample> samples = mdata.get(metricId);
        if (samples == null) {
            samples = new ArrayList<PerfSample>();
            mdata.put(metricId, samples);
        }
        return samples;
    }

    /**
     * Pops record from loader stack. Adds record to its parent child list
     * or adds record to list of loaded traces and calculates missing parameters.
     */
    private void pop() {
        if (top.getParent() != null) {
            top.getParent().addChild(top);
            top = top.getParent();
        } else {
            top.fixup(top.getTime(), 0);
            traces.add(top);
            top = new NamedTraceRecord(null);
        }
    }


    /**
     * Returns number of loaded traces.
     *
     * @return number of loaded traces
     */
    public int size() {
        return traces.size();
    }


    /**
     * Returns root record of i-th trace.
     *
     * @param i trace index
     *
     * @return root record of i-th trace
     */
    public NamedTraceRecord get(int i) {
        return traces.get(i);
    }


    /**
     * Loads data from trace file.
     *
     * @param f file path
     */
    public void load(File f) {
        traces.clear();
        symbols.clear();
        if (f.canRead()) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                long len = f.length();
                byte[] buf = new byte[(int)len];
                is.read(buf);
                SimplePerfDataFormat stf = new SimplePerfDataFormat(buf);
                stf.decode(this);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Returns symbol map.
     *
     * @return symbol map
     */
    public Map<Integer,String> getSymbols() {
        return symbols;
    }


    public String getSymbol(int id) {
        return symbols.get(id);
    }

    public Map<Integer,PerfMetricData> getMetricData() {
        return metricData;
    }

    public List<NamedTraceRecord> getTraces() {
        return traces;
    }

}
