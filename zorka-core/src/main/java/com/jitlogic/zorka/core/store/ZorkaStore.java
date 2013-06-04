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

import com.jitlogic.zorka.core.spy.TraceMarker;
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.util.*;
import org.mapdb.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicLong;

public class ZorkaStore implements RestfulService<Object> {

    private ZorkaLog log = ZorkaLogger.getLog(ZorkaStore.class);

    private String path;
    private SymbolRegistry symbols;
    private long maxFileSize, maxPhysicalSize;

    private DB traceDB;
    private TraceDataStore traceData;

    private AtomicLong lastId;
    private ConcurrentNavigableMap<Long,TraceEntry> tracesById;
    private ConcurrentNavigableMap<Long,TraceEntry> tracesByTstamp;


    public ZorkaStore(String path, long maxFileSize, long maxPhysicalSize, SymbolRegistry symbols) throws IOException {

        this.path = path;
        this.maxFileSize = maxFileSize;
        this.maxPhysicalSize = maxPhysicalSize;
        this.symbols = symbols;

        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (!dir.isDirectory()) {
            throw new IOException("Path '" + path + "' must be a directory.");
        }

    }

    public Long firstTs() {
        return tracesByTstamp.firstKey();
    }


    public Long lastTs() {
        return tracesByTstamp.lastKey();
    }


    public ConcurrentNavigableMap<Long,TraceEntry> findByTs(long from, long to) {
        return tracesByTstamp.subMap(from, true, to, true);
    }


    public TraceRecord getTrace(TraceEntry entry) throws IOException {
        byte[] blob = traceData.read(entry.getPos(), entry.getLen());
        ByteBuffer buf = new ByteBuffer(blob);

        TraceReader reader = new TraceReader();
        new SimplePerfDataFormat(buf).decode(reader);

        List<TraceRecord> result = reader.getResults();

        return result.size() > 0 ? result.get(0) : null;
    }


    public synchronized void add(TraceRecord tr) throws IOException {
        if (traceData == null || tracesById == null) {
            return;
        }

        TraceMarker marker = tr.getMarker();

        if (marker != null) {

            // TODO too much data copying back and forth. Implement TraceDataStore.write(ByteBuffer buf)
            // TODO implement proper markers for TraceData.write(...), so it is possible to recreate traceDB maps;

            ByteBuffer buf = new ByteBuffer(4096);
            tr.traverse(new SimplePerfDataFormat(buf));
            byte[] b1 = buf.getContent();
            long pos = traceData.write(b1);

            long id = lastId.incrementAndGet();
            TraceEntry entry = new TraceEntry(id, TraceEntry.FORMAT_SIMPLE, pos, b1.length,
                marker.getClock(), tr.getTime(), tr.getCalls(), tr.getErrors(), traceRecords(tr), traceLabel(tr));

            tracesById.put(entry.getId(), entry);
            tracesByTstamp.put(marker.getClock(), entry);
        } else {
            log.error(ZorkaLogger.ZCL_ERRORS, "Received trace without marker. Skipping.");
        }
    }


    private int traceRecords(TraceRecord record) {
        int ret = 1;

        for (int i = 0; i < record.numChildren(); i++) {
            ret += traceRecords(record.getChild(i));
        }

        return ret;
    }


    private String traceLabel(TraceRecord record) {
        StringBuilder sb = new StringBuilder();
        TraceMarker marker = record.getMarker();

        sb.append(symbols.symbolName(marker.getTraceId()));
        if (record.getAttrs() != null) {
            for (Map.Entry<Integer,Object> e : record.getAttrs().entrySet()) {
                sb.append('|');
                Object v = e.getValue();
                sb.append(v != null ? v.toString() : "<null>");
            }
        }

        return sb.toString();
    }


    public synchronized void open() {
        if (traceData == null) {
            try {
                traceData = new TraceDataStore(path, "trace", "dat", maxFileSize, maxPhysicalSize);
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot open trace data store", e);
            }
        }

        if (traceDB == null) {
            traceDB = DBMaker.newFileDB(new File(path, "traces.db"))
                    .randomAccessFileEnable()
                    .cacheLRUEnable()
                    .cacheSize(16384)
                    .asyncFlushDelay(1)
                    .closeOnJvmShutdown()
                    .make();
            tracesById = traceDB.getTreeMap("traces");
            tracesByTstamp = traceDB.getTreeMap("tracesByTstamp");
            lastId = new AtomicLong(tracesById.isEmpty() ? 0L : tracesById.lastKey());
        }
    }


    public synchronized void close() {
        flush();
        lastId = null;
        tracesById = null;
        tracesByTstamp = null;
        traceDB.close();
        traceDB = null;
        try {
            traceData.close();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot close trace data set", e);
        }
        traceData = null;
    }


    public void flush() {
        try {
            cleanOldEntries();

            traceData.flush();
            traceDB.commit();
            // TODO remove records from tables if traceData has been truncated
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot flush trace data set", e);
        }
    }

    private void cleanOldEntries() {
        long pos = traceData.getStartPos();

        for (Map.Entry<Long,TraceEntry> e = tracesById.firstEntry(); e != null && e.getValue().getPos() < pos; e = tracesById.firstEntry()) {
            tracesByTstamp.remove(e.getValue().getTstamp());
            tracesById.remove(e.getKey());
        }
    }

    @Override
    public Object get(String path, Map<String, String> params) {

        if (path.startsWith("/traces")) {
            // List trace entries
            int offs = ZorkaUtil.iparam(params, "offs", 0);
            int limit = ZorkaUtil.iparam(params, "limit", 50);

            List<Map> lst = new ArrayList<Map>(limit);

            if (!tracesById.isEmpty()) {
                long id = tracesById.lastKey() - offs;

                for (int i = 0; i < limit && id > 0; i++,id--) {
                    TraceEntry e = tracesById.get(id);
                    if (e != null) {
                        lst.add(ZorkaUtil.map(
                            "id", e.getId(),
                            "tstamp", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(new Date(e.getTstamp())),
                            "time", ZorkaUtil.strTime(e.getTime()),
                            "calls", e.getCalls(),
                            "errors", e.getErrors(),
                            "recs", e.getRecs(),
                            "label", e.getLabel()
                        ));
                    }
                }
            }
            return lst;
        } else {
            //
        }

        return null;
    }
}
