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
import com.jitlogic.zorka.core.util.ZorkaLogger;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

public class ZorkaStore extends ZorkaAsyncThread<Submittable> {

    private String path;
    private SymbolRegistry symbols;
    private long maxFileSize, maxPhysicalSize;

    private DB traceDB;
    private TraceDataStore traceData;

    private ConcurrentMap<Long,TraceEntry> traces;
    private ConcurrentNavigableMap<Long,Long> tracesByTstamp;


    public ZorkaStore(String path, long maxFileSize, long maxPhysicalSize, SymbolRegistry symbols) throws IOException {
        super("local-store");

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


    @Override
    protected void process(Submittable obj) {

        try {
            if (obj instanceof TraceRecord) {
                saveTraceRecord((TraceRecord) obj);
            }

            if (obj instanceof PerfRecord) {
                // TODO save performance metrics
                log.warn(ZorkaLogger.ZCL_WARNINGS, "Performance data collection not implemented. Skipping.");
            }
        } catch (IOException e) {

        }

    }


    private void saveTraceRecord(TraceRecord tr) throws IOException {
        if (traceData == null || traces == null) {
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

            traces.put(pos, entry);
            tracesByTstamp.put(marker.getClock(), pos);
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


    @Override
    protected synchronized void open() {
        if (traceData == null) {
            try {
                traceData = new TraceDataStore(path, "trace", "dat", maxFileSize, maxPhysicalSize);
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Cannot open trace data store", e);
            }
        }

        if (traceDB == null) {
            traceDB = DBMaker.newFileDB(new File(path, "traces.db"))
                    .cacheLRUEnable().cacheSize(16384).asyncFlushDelay(1).make();
            traces = traceDB.getHashMap("traces");
            tracesByTstamp = traceDB.getTreeMap("tracesByTstamp");
        }
    }


    @Override
    protected synchronized void close() {
        flush();
        traces = null;
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


    @Override
    protected void flush() {
        try {
            traceData.flush();
            traceDB.commit();
            // TODO remove records from tables if traceData has been truncated
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot flush trace data set", e);
        }
    }
}
