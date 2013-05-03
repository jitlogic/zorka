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

package com.jitlogic.zorka.core.spy;


import com.jitlogic.zorka.core.perfmon.Metric;
import com.jitlogic.zorka.core.perfmon.MetricTemplate;
import com.jitlogic.zorka.core.store.MetricsRegistry;
import com.jitlogic.zorka.core.perfmon.PerfDataEventHandler;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import com.jitlogic.zorka.core.util.*;

import java.util.List;
import java.util.Map;

/**
 * This trace event handler can be plugged between trace event sender and receiver.
 * It will check if symbol IDs in trace events coming from sender are known to receiver.
 * If not, it will send newSymbol() event with proper symbol name and ID prior to sending
 * event containing such unknown symbol ID.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SymbolEnricher extends PerfDataEventHandler {

    /** Logger object */
    private static final ZorkaLog log = ZorkaLogger.getLog(SymbolEnricher.class);


    /** Symbol ID bit mask. Zeroed bits in this mask mark symbols that are not yet known to receiver. */
    BitVector symbolsSent = new BitVector();

    BitVector templatesSent = new BitVector(16);

    BitVector metricsSent = new BitVector(16);

    /** Symbol registry used by event sender. */
    private SymbolRegistry symbols;

    private MetricsRegistry metricRegistry;

    /** Event receiver (output) object. */
    private PerfDataEventHandler output;


    /**
     * Creates new symbol enricher object.
     *
     * @param symbols symbol registry used by event sender
     *
     * @param output event receiver object
     */
    public SymbolEnricher(SymbolRegistry symbols, MetricsRegistry metricRegistry, PerfDataEventHandler output) {
        this.symbols = symbols;
        this.metricRegistry = metricRegistry;
        this.output = output;
    }


    /**
     * Checks if symbol of given ID has been sent to receiver. If not, proper symbol is sent.
     *
     * @param id symbol ID
     */
    private void check(int id) {

        if (!symbolsSent.get(id)) {
            String sym = symbols.symbolName(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with symbol '%s', id=%s", sym, id);
            output.newSymbol(id, sym);
            symbolsSent.set(id);
        }
    }


    private void checkMetric(int id) {
        if (!metricsSent.get(id)) {
            Metric metric = metricRegistry.getMetric(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + metric + "', id=" + id);
            this.newMetric(metric);
            metricsSent.set(id);
        }
    }


    private void checkMetricTemplate(int id) {
        if (!templatesSent.get(id)) {
            MetricTemplate template = metricRegistry.getTemplate(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + template + "', id=" + id);
            this.newMetricTemplate(template);
            templatesSent.set(id);
        }
    }


    /**
     * Resets enricher. Since reset enricher will forget about all sent symbol IDs
     * and will start sending (and memoizing) symbols once again.
     */
    public void reset() {
        log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Resetting symbol enricher.");
        symbolsSent.reset();
        templatesSent.reset();
        metricsSent.reset();
    }


    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        check(traceId);
        output.traceBegin(traceId, clock, flags);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        check(classId);
        check(methodId);
        check(signatureId);
        output.traceEnter(classId, methodId, signatureId, tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        output.traceReturn(tstamp);
    }


    @Override
    public void traceError(Object exception, long tstamp) {
            checkSymbolicException((SymbolicException)exception);
            output.traceError(exception, tstamp);
    }

    private void checkSymbolicException(SymbolicException sex) {
        check(sex.getClassId());
        for (SymbolicStackElement sse : sex.getStackTrace()) {
            check(sse.getClassId());
            check(sse.getMethodId());
            check(sse.getFileId());
        }

        if (sex.getCause() != null) {
            checkSymbolicException(sex.getCause());
        }
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        output.traceStats(calls, errors, flags);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        check(symbolId);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        check(attrId);
        output.newAttr(attrId, attrVal);
    }

    @Override
    public void disable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void enable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void perfData(long clock, int scannerId, List<PerfSample> samples) {
        check(scannerId);
        for (PerfSample sample : samples) {
            checkMetric(sample.getMetricId());
            if (sample.getAttrs() != null) {
                for (Map.Entry<Integer,String> e : sample.getAttrs().entrySet()) {
                    check(e.getKey());
                }
            }
        }
        output.perfData(clock, scannerId, samples);
    }


    @Override
    public void newMetricTemplate(MetricTemplate template) {
        output.newMetricTemplate(template);
    }


    @Override
    public void newMetric(Metric metric) {
        checkMetricTemplate(metric.getTemplate().getId());
        output.newMetric(metric);
    }
}
