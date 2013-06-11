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
package com.jitlogic.zorka.viewer;


import com.jitlogic.zorka.core.store.TraceRecord;
import com.jitlogic.zorka.core.store.FressianTraceFormat;
import com.jitlogic.zorka.core.store.Symbol;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import org.fressian.FressianReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraceDataSet {

    private SymbolRegistry symbols = new SymbolRegistry();
    private List<ViewerTraceRecord> traceRecords = new ArrayList<ViewerTraceRecord>();


    public TraceDataSet(File file) {

        FressianTraceFormat.TraceRecordBuilder oldb = FressianTraceFormat.TRACE_RECORD_BUILDER;

        FressianTraceFormat.TraceRecordBuilder newb = new FressianTraceFormat.TraceRecordBuilder() {
            @Override public TraceRecord get() {
                return new ViewerTraceRecord(symbols);
            }
        };

        FressianTraceFormat.TRACE_RECORD_BUILDER = newb;

        load(file);

        FressianTraceFormat.TRACE_RECORD_BUILDER = oldb;

    }

    public SymbolRegistry getSymbols() {
        return symbols;
    }

    private void load(File file) {
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(file));
            FressianReader r = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
            for (Object obj = r.readObject(); obj != null; obj = r.readObject()) {
                if (obj instanceof Symbol) {
                    Symbol sym = (Symbol)obj;
                    symbols.put(sym.getId(), sym.getName());
                } else if (obj instanceof ViewerTraceRecord) {
                    ((ViewerTraceRecord)obj).fixup();
                    traceRecords.add((ViewerTraceRecord)obj);
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


    public List<ViewerTraceRecord> getRecords() {
        return Collections.unmodifiableList(traceRecords);
    }


    public int numRecords() {
        return traceRecords.size();
    }

}
