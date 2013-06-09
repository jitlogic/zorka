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

import com.jitlogic.zorka.core.util.*;
import org.fressian.FressianWriter;
import org.fressian.Writer;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Serializes trace data in Fressian format.
 */
public class FressianTraceWriter implements MetadataChecker {

    private ZorkaLog log = ZorkaLogger.getLog(FressianTraceWriter.class);

    /** Symbol registry used by event sender. */
    private SymbolRegistry symbols;

    private MetricsRegistry metrics;

    BitVector symbolsSent = new BitVector(),  metricsSent = new BitVector(16), templatesSent = new BitVector(16);

    private OutputFactory factory;

    private OutputStream output;

    private Writer writer;

    public FressianTraceWriter(OutputFactory factory, SymbolRegistry symbols, MetricsRegistry metrics) {
        this.factory = factory;
        this.symbols = symbols;
        this.metrics = metrics;
    }


    public void write(Submittable record) {
        checkOutput();

        try {
            // Look for metadata
            record.traverse(this);

            // Serialize record in tagged fressian format
            writer.writeObject(record);
        } catch (IOException e) {
            handleError("Cannot process submitted record.", e);
        }
    }


    public void reset() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                // TODO should we go on if output stream does not close properly ?
                log.error(ZorkaLogger.ZSP_SUBMIT, "Cannot close output stream.", e);
            }
        }
        output = factory.getOutput();
        symbolsSent.reset();
        metricsSent.reset();
        templatesSent.reset();
        this.writer = new FressianWriter(output, FressianTraceFormat.WRITE_LOOKUP);
    }


    private void handleError(String msg, Throwable e) {
        log.error(ZorkaLogger.ZSP_SUBMIT, msg, e);
        reset();
    }

    private void checkOutput() {
        if (output == null) {
            reset();
        }
    }

    @Override
    public void checkSymbol(int id) {
        checkOutput();
        if (!symbolsSent.get(id)) {
            String sym = symbols.symbolName(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with symbol '%s', id=%s", sym, id);
            try {
                writer.writeObject(new Symbol(id, sym));
            } catch (IOException e) {
                handleError("Cannot submit symbol.", e);
            }
            symbolsSent.set(id);
        }
    }


    @Override
    public void checkMetric(int id) {
        checkOutput();
        if (!metricsSent.get(id)) {
            Metric metric = metrics.getMetric(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + metric + "', id=" + id);
            checkTemplate(metric.getTemplateId());
            try {
                writer.writeObject(metric);
            } catch (IOException e) {
                handleError("Cannot submit metric.", e);
            }
            metricsSent.set(id);
        }
    }


    public void checkTemplate(int id) {
        checkOutput();
        if (!templatesSent.get(id)) {
            MetricTemplate template = metrics.getTemplate(id);
            log.debug(ZorkaLogger.ZTR_SYMBOL_ENRICHMENT, "Enriching output stream with metric '" + template + "', id=" + id);
            try {
                writer.writeObject(template);
            } catch (IOException e) {
                handleError("Cannot submit metric template.", e);
            }
            templatesSent.set(id);
        }
    }

}
