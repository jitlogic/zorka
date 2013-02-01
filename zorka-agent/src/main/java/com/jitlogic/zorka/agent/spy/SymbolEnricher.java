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

package com.jitlogic.zorka.agent.spy;


import com.jitlogic.zorka.common.*;

/**
 * This trace event handler can be plugged between trace event sender and receiver.
 * It will check if symbol IDs in trace events coming from sender are known to receiver.
 * If not, it will send newSymbol() event with proper symbol name and ID prior to sending
 * event containing such unknown symbol ID.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SymbolEnricher extends TraceEventHandler {


    /** Logger object */
    private static final ZorkaLog log = ZorkaLogger.getLog(SymbolEnricher.class);


    /** Symbol ID bit mask. Zeroed bits in this mask mark symbols that are not yet known to receiver. */
    BitVector bitVector = new BitVector();


    /** Symbol registry used by event sender. */
    private SymbolRegistry symbols;


    /** Event receiver (output) object. */
    private TraceEventHandler output;


    /**
     * Creates new symbol enricher object.
     *
     * @param symbols symbol registry used by event sender
     *
     * @param output event receiver object
     */
    public SymbolEnricher(SymbolRegistry symbols, TraceEventHandler output) {
        this.symbols = symbols;
        this.output = output;
    }


    /**
     * Checks if symbol of given ID has been sent to receiver. If not, proper symbol is sent.
     *
     * @param id symbol ID
     */
    private void check(int id) {

        if (!bitVector.get(id)) {
            String sym = symbols.symbolName(id);
            if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_SYMBOL_ENRICHMENT)) {
                log.debug("Enriching output stream with symbol '" + sym + "', id=" + id);
            }
            output.newSymbol(id, sym);
            bitVector.set(id);
        }
    }


    /**
     * Resets enricher. Since reset enricher will forget about all sent symbol IDs
     * and will start sending (and memoizing) symbols once again.
     */
    public void reset() {
        if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_SYMBOL_ENRICHMENT)) {
            log.debug("Resetting symbol enricher.");
        }
        bitVector.reset();
    }


    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        check(traceId);
        output.traceBegin(traceId, clock, flags);
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
    public void traceError(Object exception, long tstamp) {
            checkSymbolicException((SymbolicException)exception);
            output.traceError(exception, tstamp);
    }

    private void checkSymbolicException(SymbolicException sex) {
        check(sex.getClassId());
        for (SymbolicStackElement sse : sex.getStackTrace()) {
            check(sse.getClassId());
            check(sse.getMethodId());
            check(sse.getFileId());
        }

        if (sex.getCause() != null) {
            checkSymbolicException(sex.getCause());
        }
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        output.traceStats(calls, errors, flags);
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


    @Override
    public void longVals(long clock, int objId, int[] components, long[] values) {
        check(objId);
        for (int component : components) {
            check(component);
        }
    }


    @Override
    public void doubleVals(long clock, int objId, int[] components, double[] values) {
        check(objId);
        for (int component : components) {
            check(component);
        }
    }
}
