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

package com.jitlogic.zorka.spy;


import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

public class SymbolEnricher extends TraceEventHandler {

    private static final ZorkaLog log = ZorkaLogger.getLog(SymbolEnricher.class);

    private long[] mask;

    private SymbolRegistry symbols;
    private TraceEventHandler output;


    public SymbolEnricher(SymbolRegistry symbols, TraceEventHandler output) {
        this.symbols = symbols;
        this.output = output;

        mask = new long[(symbols.lastId()+63)>>6];
    }


    private void check(int id) {
        int idx = id >> 6;
        int bit = id & 63;

        if (idx >= mask.length) {
            mask = ZorkaUtil.clipArray(mask, idx+1);
        }

        if (0 == (mask[idx] & (1 << bit))) {
            String sym = symbols.symbolName(id);
            if (SpyInstance.isDebugEnabled(SpyLib.SPD_TRACE_DEBUG)) {
                log.debug("Adding symbol '" + sym + "' <- " + id);
            }
            output.newSymbol(id, sym);
            mask[idx] |= (1 << bit);
        }

    }


    public void reset() {
        if (SpyInstance.isDebugEnabled(SpyLib.SPD_TRACE_DEBUG)) {
            log.debug("Resetting symbol enricher.");
        }
        for (int i = 0; i < mask.length; i++) {
            mask[i] = 0;
        }
    }


    @Override
    public void traceBegin(int traceId, long clock) {
        check(traceId);
        output.traceBegin(traceId, clock);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        check(classId);
        check(methodId);
        check(signatureId);
        output.traceEnter(classId, methodId, signatureId, tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        output.traceReturn(tstamp);
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        if (exception instanceof WrappedException) {
            SymbolicException sex = new SymbolicException(((WrappedException) exception).getException(), symbols);
            check(sex.getClassId());
            output.traceError(sex, tstamp);
        } else {
            output.traceError(exception, tstamp);
        }
    }


    @Override
    public void traceStats(long calls, long errors) {
        output.traceStats(calls, errors);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        check(symbolId);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        check(attrId);
        output.newAttr(attrId, attrVal);
    }
}
