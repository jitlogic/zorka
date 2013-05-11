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
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.util.ByteBuffer;
import com.jitlogic.zorka.core.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.util.ZorkaLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Store extends ZorkaAsyncThread<Submittable> {

    private String path;

    private ByteBuffer buf;
    private SimplePerfDataFormat format;

    private FileOutputStream traceDataFile, traceIndexFile;

    private SymbolRegistry symbols;

    private long maxSize;
    private long maxArchiveSize;

    public Store(SymbolRegistry symbols, long maxSize, long maxArchiveSize) {
        super("local-store");
        this.symbols = symbols;
        this.maxSize = maxSize;
        this.maxArchiveSize = maxArchiveSize;
        buf = new ByteBuffer(16384);
        format = new SimplePerfDataFormat(buf);
    }


    @Override
    protected void process(Submittable obj) {

        try {
            if (obj instanceof TraceRecord) {
                saveTraceRecord((TraceRecord) obj);
            }

            if (obj instanceof PerfRecord) {
                // TODO save performance metrics
                log.warn(ZorkaLogger.ZCL_WARNINGS, "Performance data collection not implemented.");
            }
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Problem collecting data record for '" + path + "' store.", e);
        }

    }

    private void saveTraceRecord(TraceRecord tr) throws IOException {
        if (traceDataFile != null && traceIndexFile != null) {
            buf.reset();
            tr.traverse(format);

            long start = traceDataFile.getChannel().position();
            byte[] data = buf.getContent();

            if (start+data.length > maxSize) {
                reopen(true);
            }

            TraceEntry entry = new TraceEntry(0, start, data.length, tr, symbols);
            byte[] index = entry.serialize();

            write(data, index);
        }
    }

    private void write(byte[] data, byte[] index) throws IOException {
        traceDataFile.write(data);
        traceIndexFile.write(index);
    }

    private void reopen(boolean rotate) {
        String tag = ""+System.currentTimeMillis();

        try {
            traceDataFile = new FileOutputStream(path + File.separatorChar + "trace-" + tag + ".dat");
            traceIndexFile = new FileOutputStream(path + File.separatorChar + "trace-" + tag + ".idx");
        } catch (IOException e) {
            log.error(ZorkaLogger.ZCL_ERRORS, "Cannot open trace file: ", e);
        }
    }

    @Override
    protected synchronized void open() {
        reopen(false);
    }

    @Override
    protected synchronized void close() {
        if (traceDataFile != null) {
            try {
                traceDataFile.close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Error closing trace data file.", e);
            }
        }
        if (traceIndexFile != null) {
            try {
                traceIndexFile.close();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Error closing trace index file.", e);
            }
        }
    } // close()


    @Override
    protected void flush() {
        if (traceDataFile != null) {
            try {
                traceDataFile.flush();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Error flushing trace data file.", e);
            }
        }

        if (traceIndexFile != null) {
            try {
                traceIndexFile.flush();
            } catch (IOException e) {
                log.error(ZorkaLogger.ZCL_ERRORS, "Error flushing trace data file.", e);
            }
        }
    } // flush()
}
