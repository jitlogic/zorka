/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.tracer;

import com.jitlogic.zorka.util.ByteBuffer;
import com.jitlogic.zorka.util.ZorkaAsyncThread;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.io.*;

public class TraceFileWriter extends ZorkaAsyncThread<TraceElement> {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SymbolRegistry symbols;
    private SymbolEnricher enricher;
    private SimpleTraceFormat encoder;
    private ByteBuffer buffer;

    private int maxFiles;
    private long maxFileSize;

    private File traceFile;
    private OutputStream stream;
    private long curSize;


    public TraceFileWriter(String path, SymbolRegistry symbols) {
        this(path, symbols, 8, 4 * 1024 * 1024);
    }


    public TraceFileWriter(String path, SymbolRegistry symbols, int maxFiles, long maxFileSize) {
        super("trace-writer");
        this.symbols = symbols;
        this.buffer = new ByteBuffer(2048);
        this.encoder = new SimpleTraceFormat(buffer);
        this.enricher = new SymbolEnricher(symbols, encoder);
        this.traceFile = new File(path);
        this.maxFiles = maxFiles;
        this.maxFileSize = maxFileSize;
    }


    @Override
    protected void process(TraceElement obj) {
        obj.traverse(enricher);
        byte[] buf = buffer.getContent();

        if (buf.length > maxFileSize) {
            log.error("Skipping too big trace: size=" + buf.length + ", maxSize=" + maxFileSize);
            return;
        }

        curSize += buf.length;

        if (curSize > maxFileSize) {
            roll();
            process(obj);
        } else {
            try {
                if (stream != null) {
                    stream.write(buf);
                }
            } catch (IOException e) {
                log.error("Cannot write to trace file " + traceFile, e);
            }
        }

        // TODO implement unit test exposing lack of reset() call
        buffer.reset();
    }


    protected void open() {
        roll();
    }


    private void roll() {

        if (stream != null) {
            close();
        }

        File f = new File(traceFile.getPath() + "." + maxFiles);
        if (f.exists()) {
            f.delete();
        }

        for (int i = maxFiles-1; i >= 0; i--) {
            f = new File(traceFile.getPath() + "." + i);
            if (f.exists()) {
                File nf = new File(traceFile.getPath() + "." + (i+1));
                f.renameTo(nf);
            }
        }

        traceFile.renameTo(new File(traceFile.getPath() + ".0"));

        try {
            stream = new BufferedOutputStream(new FileOutputStream(traceFile));
        } catch (FileNotFoundException e) {
            log.error("Cannot open trace file " + traceFile, e);
        }

        enricher.reset();
    }


    protected void close() {
        try {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        } catch (IOException e) {
            log.error("Cannot close trace file " + traceFile, e);
        }
    }


    protected void flush() {
        try {
            stream.flush();
        } catch (IOException e) {
            log.error("Cannot flush trace file " + traceFile, e);
        }
        ;
    }
}
