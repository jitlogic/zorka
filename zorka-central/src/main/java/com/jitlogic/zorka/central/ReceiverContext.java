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


import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import org.fressian.FressianWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReceiverContext implements MetadataChecker, ZicoDataProcessor {

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
                //log.warn("Unsupported object type:" + obj.getClass());
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

        StringBuilder overview = new StringBuilder();

        if (tr.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                if (overview.length() > 250) {
                    break;
                }
                if (overview.length() > 0) {
                    overview.append("|");
                }
                overview.append(e.getValue());
            }
        }


        jdbc.update("insert into TRACES (HOST_ID,DATA_OFFS,TRACE_ID,DATA_LEN,CLOCK,RFLAGS,TFLAGS,CALLS,"
                + "ERRORS,RECORDS,EXTIME,OVERVIEW) " + "values(?,?,?,?,?,?,?,?,?,?,?,?)",
                hostId,
                offs,
                tr.getMarker().getTraceId(),
                length,
                tr.getClock(),
                tr.getFlags(),
                tr.getMarker().getFlags(),
                tr.getCalls(),
                tr.getErrors(),
                numRecords(tr),
                tr.getTime(),
                overview.length() > 250 ? overview.toString().substring(0, 250) : overview.toString());
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
