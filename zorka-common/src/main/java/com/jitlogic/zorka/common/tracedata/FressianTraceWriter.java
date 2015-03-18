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

import com.jitlogic.zorka.common.util.BitVector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import org.fressian.FressianWriter;
import org.fressian.Writer;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Serializes trace data in Fressian format.
 */
public class FressianTraceWriter implements MetadataChecker, TraceWriter {

    private static ZorkaLog log = ZorkaLogger.getLog(FressianTraceWriter.class);

    /**
     * Symbol registry used by event sender.
     */
    private SymbolRegistry symbols;

    private MetricsRegistry metrics;

    BitVector symbolsSent = new BitVector(), metricsSent = new BitVector(16), templatesSent = new BitVector(16);

    private TraceStreamOutput output;

    private OutputStream os;

    private Writer writer;

    public FressianTraceWriter(SymbolRegistry symbols, MetricsRegistry metrics) {
        this.symbols = symbols;
        this.metrics = metrics;
    }


    @Override
    public void write(SymbolicRecord record) throws IOException {
        checkOutput();
        record.traverse(this);
        writer.writeObject(record);
    }


    @Override
    public void setOutput(TraceStreamOutput output) {
        this.output = output;
    }


    public void softReset() {
        writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
    }


    public void reset() {
        os = output.getOutputStream();
        symbolsSent.reset();
        metricsSent.reset();
        templatesSent.reset();
        this.writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
    }


    private void checkOutput() {
        if (os == null) {
            reset();
        }
    }


    @Override
    public int checkSymbol(int id, Object parent) throws IOException {
        checkOutput();
        if (!symbolsSent.get(id)) {
            String sym = symbols.symbolName(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with symbol '%s', id=%s", sym, id);
            writer.writeObject(new Symbol(id, sym));
            symbolsSent.set(id);
        }
        return id;
    }


    @Override
    public void checkMetric(int id) throws IOException {
        checkOutput();
        if (!metricsSent.get(id)) {
            Metric metric = metrics.getMetric(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + metric + "', id=" + id);
            checkTemplate(metric.getTemplateId());
            writer.writeObject(metric);
            metricsSent.set(id);
        }
    }


    public void checkTemplate(int id) throws IOException {
        checkOutput();
        if (!templatesSent.get(id)) {
            MetricTemplate template = metrics.getTemplate(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + template + "', id=" + id);
            writer.writeObject(template);
            templatesSent.set(id);
        }
    }

}
