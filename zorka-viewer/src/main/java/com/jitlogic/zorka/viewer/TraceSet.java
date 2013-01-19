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

import com.jitlogic.zorka.spy.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TraceSet extends TraceEventHandler {

    private SymbolRegistry symbols = new SymbolRegistry();
    private List<NamedTraceRecord> traces = new ArrayList<NamedTraceRecord>();

    private NamedTraceRecord top = new NamedTraceRecord(symbols, null);


    @Override
    public void traceBegin(int traceId, long clock) {
        top.setMarker(new TraceMarker(null, top, traceId, clock));
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

        if (top.getClassId() != 0) {
            top = new NamedTraceRecord(symbols, top);
        }

        top.setClassId(classId);
        top.setMethodId(methodId);
        top.setSignatureId(signatureId);
        top.setTime(tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        top.setTime(tstamp-top.getTime());
        pop();
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        top.setException(exception);
        top.setTime(tstamp-top.getTime());
        pop();
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        top.setCalls(calls);
        top.setErrors(errors);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        symbols.put(symbolId, symbolText);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        top.setAttr(attrId, attrVal);
    }


    private void pop() {
        if (top.getParent() != null) {
            top.getParent().addChild(top);
            top = top.getParent();
        } else {
            traces.add(top);
            top = new NamedTraceRecord(symbols, null);
        }
    }


    public int size() {
        return traces.size();
    }


    public NamedTraceRecord get(int i) {
        return traces.get(i);
    }


    public void load(File f) {
        traces.clear();
        if (f.canRead()) {
            InputStream is = null;
            try {
                is = new FileInputStream(f);
                long len = f.length();
                byte[] buf = new byte[(int)len];
                is.read(buf);
                SimpleTraceFormat stf = new SimpleTraceFormat(buf);
                stf.decode(this);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
