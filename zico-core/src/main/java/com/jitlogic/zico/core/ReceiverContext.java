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
package com.jitlogic.zico.core;


import com.jitlogic.zico.data.HostInfo;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import org.codehaus.jackson.map.ObjectMapper;
import org.fressian.FressianWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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
    private HostInfo hostInfo;

    private int maxAttrLen = 1024;

    private ObjectMapper mapper = new ObjectMapper();


    public ReceiverContext(DataSource ds, HostStore store) {
        this.symbolRegistry = store.getStoreManager().getSymbolRegistry();
        this.traceDataStore = store.getRds();
        this.jdbc = new JdbcTemplate(ds);
        this.hostInfo = store.getHostInfo();
    }


    @Override
    public synchronized void process(Object obj) throws IOException {
        try {
            if (obj instanceof Symbol) {
                processSymbol((Symbol) obj);
            } else if (obj instanceof TraceRecord) {
                log.debug("Processing trace record:" + obj);
                processTraceRecord((TraceRecord) obj);
            } else {
                if (obj != null) {
                    log.warn("Unsupported object type:" + obj.getClass());
                } else {
                    log.warn("Attempted processing NULL object (?)");
                }
            }
        } catch (Exception e) {
            log.error("Error processing trace record: ", e);
        }
    }


    private void processSymbol(Symbol sym) {
        int newid = symbolRegistry.symbolId(sym.getName());
        sidMap.put(sym.getId(), newid);
    }


    private void processTraceRecord(TraceRecord rec) throws IOException {
        rec.traverse(this);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(rec);
        byte[] chunk = os.toByteArray();
        long offs = traceDataStore.write(chunk);
        save(hostInfo.getId(), offs, chunk.length, rec);
    }


    public void save(int hostId, long offs, int length, TraceRecord tr) {

        Map<String, String> attrMap = new HashMap<String, String>();

        if (tr.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                String val = e.getValue() != null ? e.getValue().toString() : "";
                if (val.length() > maxAttrLen) {   // TODO configurable limit
                    val = val.substring(0, maxAttrLen);
                }
                attrMap.put(symbolRegistry.symbolName(e.getKey()), val);
            }
        }

        int status = 0;
        ;

        if (tr.getException() != null
                || tr.hasFlag(TraceRecord.EXCEPTION_PASS)
                || tr.getMarker().hasFlag(TraceMarker.ERROR_MARK)) {
            status = 1;
        }

        String attrJson = "";

        // TODO ? what if resulting JSON > 48000 bytes ?
        try {
            attrJson = mapper.writeValueAsString(attrMap);
        } catch (IOException e) {
            log.error("Error serializing attributes", e);
        }

        String exJson = null;

        SymbolicException e = tr.findException();

        if (e != null) {
            try {
                exJson = mapper.writeValueAsString(ZicoUtil.extractSymbolicExceptionInfo(symbolRegistry, e));
            } catch (IOException e1) {
                log.error("Error serializing exception info", e);
            }
        }

        // TODO ? what if resulting JSON > 16000 bytes ?

        jdbc.update("insert into TRACES (HOST_ID,DATA_OFFS,TRACE_ID,DATA_LEN,CLOCK,RFLAGS,TFLAGS,STATUS,"
                + "CLASS_ID,METHOD_ID,SIGN_ID,CALLS,ERRORS,RECORDS,EXTIME,ATTRS,EXINFO) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                hostId,
                offs,
                tr.getMarker().getTraceId(),
                length,
                tr.getClock(),
                tr.getFlags(),
                tr.getMarker().getFlags(),
                status,
                tr.getClassId(),
                tr.getMethodId(),
                tr.getSignatureId(),
                tr.getCalls(),
                tr.getErrors(),
                numRecords(tr),
                tr.getTime(),
                attrJson,
                exJson
        );
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

    public HostInfo getHostInfo() {
        return hostInfo;
    }
}
