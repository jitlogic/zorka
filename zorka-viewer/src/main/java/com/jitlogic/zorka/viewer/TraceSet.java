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

package com.jitlogic.zorka.viewer;

import com.jitlogic.zorka.tracer.SimpleTraceFormat;
import com.jitlogic.zorka.tracer.SymbolRegistry;
import com.jitlogic.zorka.tracer.TraceEventHandler;
import com.jitlogic.zorka.tracer.TracedException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TraceSet extends TraceEventHandler {

    private SymbolRegistry symbols = new SymbolRegistry();
    private List<NamedTraceElement> traces = new ArrayList<NamedTraceElement>();

    private NamedTraceElement top = new NamedTraceElement(symbols, null);


    @Override
    public void traceBegin(int traceId, long clock) {
        top.setTraceId(traceId);
        top.setClock(clock);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

        if (top.isBusy()) {
            top = new NamedTraceElement(symbols, top);
        }

        top.setClassId(classId);
        top.setMethodId(methodId);
        top.setSignatureId(signatureId);
        top.setTstart(tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        top.setTstop(tstamp);
        pop();
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        top.setException(exception);
        top.setTstop(tstamp);
        pop();
    }


    @Override
    public void traceStats(long calls, long errors) {
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
            top = new NamedTraceElement(symbols, null);
        }
    }


    public int size() {
        return traces.size();
    }


    public NamedTraceElement get(int i) {
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
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                return;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }
    }
}
