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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.central.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import org.fressian.FressianWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReceiverContext implements MetadataChecker, ZicoDataProcessor {

    private final static Logger log = LoggerFactory.getLogger(ReceiverContext.class);

    private SymbolRegistry symbolRegistry;
    private Map<Integer, Integer> sidMap = new HashMap<Integer, Integer>();

    private RDSStore traceDataStore;
    private JdbcTemplate jdbc;
    private int hostId;

    public ReceiverContext(JdbcTemplate jdbc, Store store) {
        this.symbolRegistry = store.getSymbolRegistry();
        this.traceDataStore = store.getRds();
        this.jdbc = jdbc;
        this.hostId = store.getHostInfo().getId();
    }


    @Override
    public synchronized void process(Object obj) throws IOException {
        if (obj instanceof Symbol) {
            processSymbol((Symbol) obj);
        } else if (obj instanceof TraceRecord) {
            processTraceRecord(hostId, (TraceRecord) obj);
        } else {
            if (obj != null) {
                log.warn("Unsupported object type:" + obj.getClass());
            }
        }
    }


    private void processSymbol(Symbol sym) {
        int newid = symbolRegistry.symbolId(sym.getName());
        sidMap.put(sym.getId(), newid);
    }


    private void processTraceRecord(int hostId, TraceRecord rec) throws IOException {
        rec.traverse(this);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(rec);
        byte[] chunk = os.toByteArray();
        long offs = traceDataStore.write(chunk);
        save(hostId, offs, chunk.length, rec);
    }


    public void save(int hostId, long offs, int length, TraceRecord tr) {

        StringBuilder description = new StringBuilder();
        description.append(symbolRegistry.symbolName(tr.getMarker().getTraceId()));

        if (tr.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                String val = e.getValue() != null ? e.getValue().toString() : "";
                if (val.length() > 32000) {
                    val = val.substring(0, 32000) + "...";
                }
                jdbc.update("insert into TRACE_ATTRS (HOST_ID,DATA_OFFS,ATTR_KEY,ATTR_VAL) values (?,?,?,?)",
                        hostId, offs, e.getKey(), val);
                if (val.length() > 50) {
                    val = val.substring(0, 50);
                }
                description.append('|');
                description.append(val);
            }
        }

        jdbc.update("insert into TRACES (HOST_ID,DATA_OFFS,TRACE_ID,DATA_LEN,CLOCK,RFLAGS,TFLAGS,STATUS,"
                + "CLASS_ID,METHOD_ID,SIGN_ID,CALLS,ERRORS,RECORDS,EXTIME,DESCRIPTION) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                hostId,
                offs,
                tr.getMarker().getTraceId(),
                length,
                tr.getClock(),
                tr.getFlags(),
                tr.getMarker().getFlags(),
                0 != (tr.getMarker().getFlags() & TraceMarker.ERROR_MARK) ? 1 : 0,
                tr.getClassId(),
                tr.getMethodId(),
                tr.getSignatureId(),
                tr.getCalls(),
                tr.getErrors(),
                numRecords(tr),
                tr.getTime(),
                description.length() <= 250 ? description.toString() : description.substring(0, 250));
    }


    private int numRecords(TraceRecord rec) {
        int n = 1;

        for (int i = 0; i < rec.numChildren(); i++) {
            n += numRecords(rec.getChild(i));
        }

        return n;
    }

    @Override
    public int checkSymbol(int symbolId) throws IOException {
        return sidMap.containsKey(symbolId) ? sidMap.get(symbolId) : 0;
    }


    @Override
    public void checkMetric(int metricId) throws IOException {
    }

}
