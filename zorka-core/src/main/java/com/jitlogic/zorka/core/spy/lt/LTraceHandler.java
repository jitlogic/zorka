/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.lt;


import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class receives loose tracer submissions from single thread
 * and constructs traces.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LTraceHandler extends TraceHandler {

    /**
     * Output
     */
    private ZorkaSubmitter<SymbolicRecord> output;

    private SymbolRegistry symbols;

    /**
     * Top of trace records stack.
     */
    private TraceRecord ttop = new TraceRecord(null);

    /**
     * Number of records collected so far
     */
    private int numRecords = 0;


    /**
     * Creates new trace builder object.
     *
     * @param output object completed traces will be submitted to
     */
    public LTraceHandler(ZorkaSubmitter<SymbolicRecord> output, SymbolRegistry symbols) {
        this.output = output;
        this.symbols = symbols;
    }


    @Override
    public void traceBegin(int traceId, long clock, int flags) {

        if (ttop == null) {
            log.error("Attempt to set trace marker on an non-traced method.");
            return;
        }

        if (ttop.hasFlag(TraceRecord.TRACE_BEGIN)) {
            log.error("Trace marker already set on current frame. Skipping.");
            return;
        } else {
            ttop.setMarker(new TraceMarker(ttop, traceId, clock));
            ttop.markFlag(TraceRecord.TRACE_BEGIN);
            ttop.getMarker().markFlags(flags);
        }
    }


    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

        if (disabled) {
            return;
        }


        if (log.isTraceEnabled()) {
            if ((ttop.inTrace() && ttop.getMarker().hasFlag(TraceMarker.TRACE_CALLS))) {
                log.trace("traceEnter(" + symbols.symbolName(classId) + "." + symbols.symbolName(methodId) + ")");
            }
        }

        if (!ttop.isEmpty()) {
            if (ttop.inTrace()) {
                ttop = new TraceRecord(ttop);
                numRecords++;
            } else {
                ttop.clean();
                numRecords = 0;
            }
        } else {
            numRecords++;
        }

        ttop.setClassId(classId);
        ttop.setMethodId(methodId);
        ttop.setSignatureId(signatureId);
        ttop.setTime(tstamp);
        ttop.setCalls(ttop.getCalls() + 1);

        if (numRecords > LTracer.getMaxTraceRecords()) {
            ttop.markFlag(TraceRecord.OVERFLOW_FLAG);
        }

    }


    public void traceReturn(long tstamp) {

        if (disabled) {
            return;
        }

        if (log.isTraceEnabled()) {
            if ((ttop.inTrace() && ttop.getMarker().hasFlag(TraceMarker.TRACE_CALLS))) {
                TraceRecord tr = ttop;
                if (tr.getClassId() == 0 && tr.getParent() != null) {
                    tr = tr.getParent();
                }
                log.trace("traceReturn(" + symbols.symbolName(tr.getClassId()) + "." + symbols.symbolName(tr.getMethodId()) + ")");
            }
        }

        while (!(ttop.getClassId() != 0) && ttop.getParent() != null) {
            ttop = ttop.getParent();
        }

        ttop.setTime(tstamp - ttop.getTime());

        pop();
    }


    public void traceError(Object exception, long tstamp) {

        if (disabled) {
            return;
        }

        if (log.isTraceEnabled()) {
            if ((ttop.inTrace() && ttop.getMarker().hasFlag(TraceMarker.TRACE_CALLS))) {
                TraceRecord tr = ttop;
                if (tr.getClassId() == 0 && tr.getParent() != null) {
                    tr = tr.getParent();
                }
                log.trace("traceError(" + symbols.symbolName(tr.getClassId()) +
                        "." + symbols.symbolName(tr.getMethodId()) + ")", (Throwable) exception);
            }
        }

        while (!(ttop.getClassId() != 0) && ttop.getParent() != null) {
            ttop = ttop.getParent();
        }

        ttop.setException(exception);
        ttop.setTime(tstamp - ttop.getTime());
        ttop.setErrors(ttop.getErrors() + 1);

        pop();
    }


    public TraceRecord realTop() {
        if (ttop.isEmpty() && ttop.getParent() != null) {
            return ttop.getParent();
        } else {
            return ttop;
        }
    }


    @Override
    public void newAttr(int traceId, int attrId, Object attrVal) {
        TraceRecord tr = realTop();

        while (tr != null) {
            if (traceId == -1 || (tr.hasFlag(TraceRecord.TRACE_BEGIN) &&
                (traceId == 0 || tr.getMarker().getTraceId() == traceId))) {
                tr.setAttr(attrId, attrVal);
                break;
            }
            tr = tr.getParent();
        }
    }


    /**
     * This method it called at method return (normal or error). In general it pops current
     * trace record from top of stack but it also implements quite a bit of logic handling
     * various aspects of handling trace records (filtering, limiting number of records in one
     * frame, reusing trace record if suitable etc.).
     */
    private void pop() {

        boolean clean = true;

        TraceRecord parent = ttop.getParent();

        popException();


        // Submit data if trace marker found
        if (ttop.hasFlag(TraceRecord.TRACE_BEGIN)) {
            int flags = ttop.getMarker().getFlags();
            if (((ttop.getTime() >= ttop.getMarker().getMinimumTime() || ttop.getCalls() >= LTracer.getMinTraceCalls())
                && 0 == (flags & TraceMarker.DROP_TRACE)) || 0 != (flags & TraceMarker.SUBMIT_TRACE)) {
                submit(ttop);
                AgentDiagnostics.inc(AgentDiagnostics.TRACES_SUBMITTED);
                clean = false;
            } else {
                AgentDiagnostics.inc(AgentDiagnostics.TRACES_DROPPED);
            }


            if (parent != null) {
                parent.getMarker().inheritFlags(ttop.getMarker().getFlags());
            }
        }

        // Determine how the top of stack should be rolled back
        if (parent != null) {
            if ((ttop.getTime() > LTracer.getMinMethodTime() || ttop.getErrors() > 0 || ttop.hasFlag(TraceRecord.FORCE_TRACE))
                    || 0 != (ttop.getMarker().getFlags() & TraceMarker.ALL_METHODS)) {


                if (!ttop.hasFlag(TraceRecord.OVERFLOW_FLAG)) {
                    reparentTop(parent);

                } else {
                    parent.getMarker().markFlags(TraceMarker.OVERFLOW_FLAG);
                }
                clean = false;
            }
            parent.setCalls(parent.getCalls() + ttop.getCalls());
            parent.setErrors(parent.getErrors() + ttop.getErrors());
        }


        if (clean) {
            ttop.clean();
            numRecords--;
        } else {
            if (parent != null) {
                ttop = parent;
            } else {
                ttop = new TraceRecord(null);
                numRecords = 0;
            }
        }

    }


    private void popException() {
        // Get rid of redundant exception object
        if (ttop.getException() != null && ttop.numChildren() > 0) {
            Object tex = ttop.getException();
            Object cex = ttop.getChild(ttop.numChildren() - 1).getException();
            if (cex == tex) {
                ttop.setException(null);
                ttop.markFlag(TraceRecord.EXCEPTION_PASS);
            } else if (cex == ((Throwable) tex).getCause()) {
                ttop.markFlag(TraceRecord.EXCEPTION_WRAP);
            }
        }
    }


    private void reparentTop(TraceRecord parent) {
        // Drop interim record if necessary
        if (ttop.getMarker().hasFlag(TraceMarker.DROP_INTERIM) && ttop.isInterimDroppable()
                && ttop.getTime() - ttop.getChild(0).getTime() < LTracer.getMinMethodTime()) {
            TraceRecord child = ttop.getChild(0);
            child.setCalls(ttop.getCalls());
            child.setErrors(ttop.getErrors());
            child.markFlag(TraceRecord.DROPPED_PARENT);
            numRecords--;
            parent.addChild(child);
        } else {
            parent.addChild(ttop);
        }
    }


    private void submit(TraceRecord record) {
        record.fixup(symbols);
        if (record.getException() != null || record.hasFlag(TraceRecord.EXCEPTION_PASS)) {
            record.getMarker().markFlags(TraceMarker.ERROR_MARK);
        }
        output.submit(record);
    }


    @Override
    public void setMinimumTraceTime(long minimumTraceTime) {
        TraceRecord top = realTop();
        if (top.inTrace()) {
            top.getMarker().setMinimumTime(minimumTraceTime);
        }
    }


    @Override
    public void markTraceFlags(int traceId, int flag) {
        for (TraceRecord tr = realTop(); tr != null; tr = tr.getParent()) {
            TraceMarker tm = tr.getMarker();
            if (tm != null && (traceId == 0 || traceId == tm.getTraceId())) {
                tm.markFlags(flag);
                break;
            }
        }
    }

    @Override
    public void markRecordFlags(int flag) {
        TraceRecord tr = realTop();
        if (tr != null) {
            tr.markFlag(flag);
        }
    }


    @Override
    public boolean isInTrace(int traceId) {
        for (TraceRecord tr = realTop(); tr != null; tr = tr.getParent()) {
            TraceMarker tm = tr.getMarker();
            if (tm != null && tm.getTraceId() == traceId) {
                return true;
            }
        }
        return false;
    }

}
