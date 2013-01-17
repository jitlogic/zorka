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

import com.jitlogic.zorka.spy.SymbolRegistry;
import com.jitlogic.zorka.spy.TraceEventHandler;
import com.jitlogic.zorka.spy.TracedException;

import java.util.Date;

public class TraceToTextConverter extends TraceEventHandler {

    private SymbolRegistry symbols;
    private StringBuilder sb = new StringBuilder();
    private int spaces = 0;


    public TraceToTextConverter(NamedTraceRecord trace) {
        this.symbols = trace.getSymbols();
        trace.traverse(this);
    }

    private void spc(int n) {
        for (int i = 0; i < n*4; i++) {
            sb.append(' ');
        }
    }

    public void traceBegin(int traceId, long clock) {
        spc(spaces);
        sb.append("BEGIN " + symbols.symbolName(traceId) + " (" + new Date(clock) + ")" + "\n");
    }


    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        spc(spaces); spaces += 2;
        sb.append("ENTER " + symbols.symbolName(classId) + "." + symbols.symbolName(methodId) + " " + symbols.symbolName(signatureId) + "\n");
    }


    public void traceReturn(long tstamp) {
        spc(spaces); spaces -= 2;
        sb.append("RETURN (t=" + tstamp + ")" + "\n");
    }


    public void traceError(TracedException exception, long tstamp) {
        spc(spaces); spaces -= 2;
        sb.append("ERROR (t=" + tstamp + ") " + exception + "\n");
    }


    public void traceStats(long calls, long errors) {
        spc(spaces);
        sb.append("STATS calls=" + calls + ", errors=" + errors + "\n");
    }


    public void newAttr(int attrId, Object attrVal) {
        spc(spaces);
        sb.append("ATTR " + symbols.symbolName(attrId) + " -> " + attrVal + "\n") ;
    }

    public String toString() {
        return sb.toString();
    }
}
