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
 * This class receives loose tracer submissions from single thread
 * and constructs traces.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceBuilder extends TraceEventHandler {

    private final static ZorkaLog log = ZorkaLogger.getLog(TraceBuilder.class);


    /** Output */
    private ZorkaAsyncThread<Submittable> output;

    private SymbolRegistry symbols;

    /** Top of trace records stack. */
    private TraceRecord ttop = new TraceRecord(null);


    /** Number of records collected so far */
    private int numRecords = 0;


    /**
     * Creates new trace builder object.
     *
     * @param output object completed traces will be submitted to
     */
    public TraceBuilder(ZorkaAsyncThread<Submittable> output, SymbolRegistry symbols) {
        this.output = output;
        this.symbols = symbols;
    }


    @Override
    public void traceBegin(int traceId, long clock, int flags) {

        if (ttop == null) {
            if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_TRACE_ERRORS)) {
                log.error("Attempt to set trace marker on an non-traced method.");
            }
            return;
        }

        if (ttop.hasFlag(TraceRecord.TRACE_BEGIN)) {
            if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_TRACE_ERRORS)) {
                log.error("Trace marker already set on current frame. Skipping.");
            }
            return;
        } else {
            ttop.setMarker(new TraceMarker(ttop, traceId, clock));
            ttop.markFlag(TraceRecord.TRACE_BEGIN);
            ttop.getMarker().markFlag(flags);
        }
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

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

        if (numRecords > Tracer.getMaxTraceRecords()) {
            ttop.markFlag(TraceRecord.OVERFLOW_FLAG);
        }

    }


    @Override
    public void traceReturn(long tstamp) {

        while (!(ttop.getClassId() != 0) && ttop.getParent() != null) {
            ttop = ttop.getParent();
        }

        ttop.setTime(tstamp - ttop.getTime());

        pop();
    }


    @Override
    public void traceError(Object exception, long tstamp) {

        while (!(ttop.getClassId() != 0) && ttop.getParent() != null) {
            ttop = ttop.getParent();
        }

        ttop.setException(exception);
        ttop.setTime(tstamp - ttop.getTime());
        ttop.setErrors(ttop.getErrors() + 1);

        pop();
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        ttop.setAttr(attrId, attrVal);
    }


    @Override
    public void longVals(long clock, int objId, int[] components, long[] values) {
    }


    @Override
    public void doubleVals(long clock, int objId, int[] components, double[] values) {
    }

    @Override
    public void newMetricTemplate(MetricTemplate template) {
    }

    @Override
    public void newMetric(Metric metric) {
    }


    /**
     * This method it called at method return (normal or error). In general it pops current
     * trace record from top of stack but it also implements quite a bit of logic handling
     * various aspects of handling trace records (filtering, limiting number of records in one
     * frame, reusing trace record if suitable etc.).
     *
     */
    private void pop() {

        boolean clean = true;

        TraceRecord parent = ttop.getParent();

        popException();


        // Submit data if trace marker found
        if (ttop.hasFlag(TraceRecord.TRACE_BEGIN)) {
            if (ttop.getTime() >= ttop.getMarker().getMinimumTime()
                    || 0 != (ttop.getMarker().getFlags() & TraceMarker.ALWAYS_SUBMIT)) {
                submit(ttop);
                clean = false;
            }


            if (parent != null) {
                parent.getMarker().inheritFlags(ttop.getMarker().getFlags());
            }
        }

        // Determine how the top of stack should be rolled back
        if (parent != null) {
            if ((ttop.getTime() > Tracer.getMinMethodTime() || ttop.getErrors() > 0)
                    || 0 != (ttop.getMarker().getFlags() & TraceMarker.ALL_METHODS)) {


                if (!ttop.hasFlag(TraceRecord.OVERFLOW_FLAG)) {
                    reparentTop(parent);

                } else {
                    parent.getMarker().markFlag(TraceMarker.OVERFLOW_FLAG);
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
            Throwable tex = (Throwable)ttop.getException();
            Throwable cex = (Throwable)ttop.getChild(ttop.numChildren()-1).getException();
            if (cex == tex) {
                ttop.setException(null);
                ttop.markFlag(TraceRecord.EXCEPTION_PASS);
            } else if (cex == tex.getCause()) {
                ttop.markFlag(TraceRecord.EXCEPTION_WRAP);
            }
        }
    }


    private void reparentTop(TraceRecord parent) {
        // Drop interim record if necessary
        if (ttop.getMarker().hasFlag(TraceMarker.DROP_INTERIM) && ttop.isSafeInterim()
                && ttop.getTime() - ttop.getChild(0).getTime() < Tracer.getMinMethodTime()) {
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
        output.submit(record);
    }


    /**
     * Sets minimum trace execution time for currently recorded trace.
     * If there is no trace being recorded just yet, this method will
     * have no effect.
     *
     * @param minimumTraceTime (in nanoseconds)
     */
    public void setMinimumTraceTime(long minimumTraceTime) {
        if (ttop.inTrace()) {
            ttop.getMarker().setMinimumTime(minimumTraceTime);
        }
    }


    public void markTraceFlag(int flag) {
        if (ttop.getMarker() != null) {
            ttop.getMarker().markFlag(flag);
        }
    }

}
