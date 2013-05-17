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

import com.jitlogic.zorka.core.perfmon.PerfRecord;
import com.jitlogic.zorka.core.perfmon.Submittable;
import com.jitlogic.zorka.core.spy.TraceMarker;
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.util.ByteBuffer;
import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLog;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import org.mapdb.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

public class ZorkaStore {

    private ZorkaLog log = ZorkaLogger.getLog(ZorkaStore.class);

    private String path;
    private SymbolRegistry symbols;
    private long maxFileSize, maxPhysicalSize;

    private DB traceDB;
    private TraceDataStore traceData;

    private ConcurrentNavigableMap<Long,TraceEntry> tracesByPos;
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



    public void add(TraceRecord tr) throws IOException {
        if (traceData == null || tracesByPos == null) {
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

            TraceEntry entry = new TraceEntry(TraceEntry.FORMAT_SIMPLE, pos, b1.length,
                marker.getClock(), tr.getTime(), tr.getCalls(), tr.getErrors(), traceRecords(tr), traceLabel(tr));

            tracesByPos.put(pos, entry);
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
                    .make();
            tracesByPos = traceDB.getTreeMap("traces");
            tracesByTstamp = traceDB.getTreeMap("tracesByTstamp");
        }
    }


    public synchronized void close() {
        flush();
        tracesByPos = null;
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
        if (tracesByPos.firstKey() < pos) {
            Set<Long> keys = new HashSet<Long>(), tstamps = new HashSet<Long>();

            for (Map.Entry<Long,TraceEntry> e : tracesByPos.headMap(pos, false).entrySet()) {
                keys.add(e.getKey());
                tstamps.add(e.getValue().getTstamp());
            }

            for (Long key : keys) {
                tracesByPos.remove(key);
            }

            for (Long tstamp : tstamps) {
                tracesByTstamp.remove(tstamp);
            }
        }
    }
}
