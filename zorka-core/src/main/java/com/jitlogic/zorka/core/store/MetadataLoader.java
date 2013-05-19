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
import com.jitlogic.zorka.core.perfmon.PerfDataEventHandler;
import com.jitlogic.zorka.core.util.PerfSample;
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.io.*;
import java.util.List;

public class MetadataLoader implements PerfDataEventHandler {

    private static ZorkaLog log = ZorkaLogger.getLog(MetadataLoader.class);

    private SymbolRegistry symbols;
    private MetricsRegistry metrics;


    public MetadataLoader(SymbolRegistry symbols, MetricsRegistry metrics) {
        this.symbols = symbols;
        this.metrics = metrics;
    }


    public void load(String path) {
        File f = new File(path);
        if (f.isFile() && f.canRead()) {
            InputStream is = null;
            byte[] b = new byte[(int)f.length()];
            try {
                is = new FileInputStream(f);
                is.read(b);
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot read input file " + path, e);
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException e) { }
                }
            }
            SimplePerfDataFormat spdf = new SimplePerfDataFormat(b);
            spdf.decode(this);
        } else {
            log.error(ZorkaLogger.ZCL_ERRORS, "File " + path + " is non-existent or non-readable.");
        }
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        symbols.put(symbolId, symbolText);
    }


    @Override
    public void newMetricTemplate(MetricTemplate template) {
        metrics.add(template);
    }


    @Override
    public void newMetric(Metric metric) {
        metrics.add(metric);
    }


    @Override public void traceStats(long calls, long errors, int flags) { }
    @Override public void perfData(long clock, int scannerId, List<PerfSample> samples) { }
    @Override public void traceBegin(int traceId, long clock, int flags) { }
    @Override public void traceEnter(int classId, int methodId, int signatureId, long tstamp) { }
    @Override public void traceReturn(long tstamp) { }
    @Override public void traceError(Object exception, long tstamp) { }
    @Override public void newAttr(int attrId, Object attrVal) { }
    @Override public void disable() { }
    @Override public void enable() { }
}
