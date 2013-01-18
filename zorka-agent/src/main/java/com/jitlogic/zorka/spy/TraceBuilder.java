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


import com.jitlogic.zorka.util.ZorkaAsyncThread;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

/**
 * This class receives loose tracer submissions from single thread
 * and constructs traces.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceBuilder extends TraceEventHandler {

    private final static ZorkaLog log = ZorkaLogger.getLog(TraceBuilder.class);

    /** Output */
    private ZorkaAsyncThread<TraceRecord> output;

    /** Top of trace markers stack. */
    private TraceMarker mtop = null;

    /** Top of trace records stack. */
    private TraceRecord ttop = new TraceRecord(null);

    private int numRecords = 0;

    public TraceBuilder(ZorkaAsyncThread<TraceRecord> output) {
        this.output = output;
    }


    @Override
    public void traceBegin(int traceId, long clock) {

        if (ttop == null) {
            log.error("Attempt to set trace marker on an non-traced method.");
            return;
        }

        if (mtop != null && mtop.getRoot().equals(ttop)) {
            log.error("Trace marker already set on current frame. Skipping.");
            return;
        }

        mtop = new TraceMarker(mtop, ttop, traceId, clock);
        ttop.setMarker(mtop);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

        if (ttop.getClassId() != 0) {
            if (mtop != null) {
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

        if (numRecords > Tracer.getDefaultTraceSize()) {
            ttop.markOverflow();
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
    public void traceError(TracedException exception, long tstamp) {

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


    private void pop() {

        boolean clean = true;

        if (ttop.getMarker() != null) {
            if (ttop.getTime() >= ttop.getMarker().getMinimumTime()) {
                output.submit(ttop);
                clean = false;
            }

            if (ttop.getMarker().equals(mtop)) {
                mtop = mtop.pop();
            } else {
                log.error("Markers didn't match on tracer stack pop.");
            }
        }

        TraceRecord parent = ttop.getParent();

        if (parent != null) {
            if ((ttop.getTime() > Tracer.getDefaultMethodTime() || ttop.getErrors() > 0)) {
                if (!ttop.hasOverflow()) {
                    parent.addChild(ttop);
                    clean = false;
                } else {
                    mtop.markOverflow();
                    clean = false;
                }
            }
            parent.setCalls(parent.getCalls() + ttop.getCalls());
            parent.setErrors(parent.getErrors() + ttop.getErrors());
        }

        if (clean) {
            ttop.clean();
            numRecords--;
        } else {
            ttop = parent != null ? parent : new TraceRecord(null);
        }

    } // pop()


    public void setMinimumTraceTime(long minimumTraceTime) {
        if (mtop != null) {
            mtop.setMinimumTime(minimumTraceTime);
        }
    }

}
