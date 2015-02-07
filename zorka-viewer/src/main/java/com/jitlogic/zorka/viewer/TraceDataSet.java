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
package com.jitlogic.zorka.viewer;


import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.fressian.FressianReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.*;

public class TraceDataSet {

    private SymbolRegistry symbols = new SymbolRegistry();
    private List<ViewerTraceRecord> traceRecords = new ArrayList<ViewerTraceRecord>();



    public TraceDataSet(File file) {

        FressianTraceFormat.TraceRecordBuilder oldb = FressianTraceFormat.TRACE_RECORD_BUILDER;
        FressianTraceFormat.TraceRecordBuilder newb = new FressianTraceFormat.TraceRecordBuilder() {
            @Override
            public TraceRecord get() {
                return new ViewerTraceRecord(symbols);
            }
        };

        synchronized (FressianTraceFormat.class) {
            FressianTraceFormat.TRACE_RECORD_BUILDER = newb;
            try {
                load(file);
            } finally {
                FressianTraceFormat.TRACE_RECORD_BUILDER = oldb;
            }
        }

    }

    public SymbolRegistry getSymbols() {
        return symbols;
    }


    private void load(File file) {
        InputStream is = null;

        try {
            is = open(file);
            FressianReader r = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            for (Object obj = r.readObject(); obj != null; obj = r.readObject()) {
                if (obj instanceof Symbol) {
                    Symbol sym = (Symbol) obj;
                    symbols.put(sym.getId(), sym.getName());
                } else if (obj instanceof ViewerTraceRecord) {
                    ((ViewerTraceRecord) obj).fixup();
                    traceRecords.add((ViewerTraceRecord) obj);
                } else {
                    System.err.println("Unknown object: " + obj);
                }
            }
        } catch (EOFException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private InputStream open(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] hdr = new byte[4];
            fis.read(hdr);

            if (hdr[0] != 'Z' || hdr[1] != 'T' || hdr[2] != 'R') {
                throw new IOException("Invalid header (invalid file type).");
            }

            if (hdr[3] == 'Z') {
                InputStream is = new BufferedInputStream(new InflaterInputStream(fis, new Inflater(true), 65536));
                return is;
            } else if (hdr[3] == 'C') {
                return new BufferedInputStream(fis);
            } else {
                throw new IOException("Invalid header (invalid file type).");
            }
        } catch (Exception e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                }
            }
        }
        return null;
    }


    public List<ViewerTraceRecord> getRecords() {
        return Collections.unmodifiableList(traceRecords);
    }


    public int numRecords() {
        return traceRecords.size();
    }

}
