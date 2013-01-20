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

import com.jitlogic.zorka.spy.TraceEventHandler;
import com.jitlogic.zorka.spy.TracedException;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is simple standalone trace printer. It receives stream of trace events
 * and prints them in human-readable form to a print stream.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TracePrinter extends TraceEventHandler {

    private PrintStream out;

    private Map<Integer,String> symbols = new HashMap<Integer,String>();

    private int level;

    /**
     * Creates new trace printer object.
     *
     * @param out output stream
     */
    public TracePrinter(PrintStream out) {
        this.out = out;
    }


    private String sym(int id) {
        return symbols.containsKey(id) ? symbols.get(id) : "<? id=" + id + ">";
    }


    private String spc(int level) {
        StringBuilder sb = new StringBuilder(level+2);
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }


    private String time(long tstamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(tstamp));
    }


    @Override
    public void traceBegin(int traceId, long clock) {
        out.println(spc(level) + "TRACE_BEGIN: " + sym(traceId) + " clock=" + new Date(clock));
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        out.println(spc(level) + "ENTER (" + time(tstamp) + "): " + ViewerUtil.methodString(sym(classId), sym(methodId), sym(signatureId)));
        level++;
    }


    @Override
    public void traceReturn(long tstamp) {
        out.println(spc(level) + "RETURN (" + time(tstamp) + ")");
        if (level > 0) {
            level--;
        }
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        out.println(spc(level) + "ERROR (" + time(tstamp) + "): " + exception);
        if (level > 0) {
            level--;
        }
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        out.println(spc(level) + "calls=" + calls + ", errors=" + errors + ", flags=" + flags);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        out.println("SYMBOL: symbolId=" + symbolId + ", text='" + symbolText + "'");
        symbols.put(symbolId, symbolText);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        out.println(spc(level) + sym(attrId) + "=" + attrVal);
    }
}
