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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReceiverContext implements MetadataChecker, ZicoDataProcessor {

    //private static Logger log = LoggerFactory.getLogger(ReceiverContext.class);

    private SymbolSet symbolSet;
    private Map<Integer,Integer> sidMap = new HashMap<Integer,Integer>();

    private TraceEntrySet traceEntrySet;
    private RDSStore traceDataStore;

    private int unknownSid;


    public ReceiverContext(Store store) {
        this.symbolSet = store.getSymbols();
        this.traceEntrySet = store.getTraces();
        this.traceDataStore = store.getRds();
        this.unknownSid = symbolSet.get("<UNKNOWN>");
    }


    @Override
    public synchronized void process(Object obj) throws IOException {
        if (obj instanceof Symbol) {
            processSymbol((Symbol)obj);
        } else if (obj instanceof TraceRecord) {
            processTraceRecord((TraceRecord) obj);
        } else {
            if (obj != null) {
                //log.warn("Unsupported object type:" + obj.getClass());
            }
        }
    }


    private void processSymbol(Symbol sym) {
        int newid = symbolSet.get(sym.getName());
        sidMap.put(sym.getId(), newid);
    }


    private void processTraceRecord(TraceRecord rec) throws IOException {
        rec.traverse(this);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FressianWriter writer = new FressianWriter(os, FressianTraceFormat.WRITE_LOOKUP);
        writer.writeObject(rec);
        byte[] chunk = os.toByteArray();
        long offs = traceDataStore.write(chunk);
        traceEntrySet.save(new TraceEntry(offs, chunk.length, symbolSet, rec));
    }


    @Override
    public int checkSymbol(int symbolId) throws IOException {
        return sidMap.containsKey(symbolId) ? sidMap.get(symbolId) : unknownSid;
    }


    @Override
    public void checkMetric(int metricId) throws IOException {
    }

}
