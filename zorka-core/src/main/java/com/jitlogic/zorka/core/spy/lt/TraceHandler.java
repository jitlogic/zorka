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

import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.tuner.TraceDetailStats;
import com.jitlogic.zorka.core.spy.tuner.TraceSummaryStats;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TraceHandler {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected boolean disabled;

    protected long tunCalls = 0;
    protected long tunDrops = 0;
    protected long tunErrors = 0;
    protected long tunLCalls = 0;

    protected TracerTuner tuner;
    protected TraceSummaryStats tunStats = null;

    protected long tunLastExchange = 0;

    public abstract void traceBegin(int traceId, long clock, int flags);

    /**
     * Attaches attribute to current trace record (or any other record up the call stack).
     *
     * @param traceId positive number (trace id) if attribute has to be attached to a top record of specific
     *                trace, 0 if attribute has to be attached to a top record of any trace, -1 if attribute
     *                has to be attached to current method;
     * @param attrId  attribute ID
     * @param attrVal attribute value
     */
    public abstract void newAttr(int traceId, int attrId, Object attrVal);

    public void disable() {
        disabled = true;
    }

    public void enable() {
        disabled = false;
    }

    protected void tuningProbe(int mid, long tstamp, long ttime) {

        if (tunStats == null || tstamp > tunLastExchange + Tracer.getTuningDefaultExchInterval()) {
            tuningExchange(tstamp);
        }

        tunCalls++;
        if (ttime < LTracer.getMinMethodTime()) tunDrops++;

        if (Tracer.getTuningMode() == Tracer.TUNING_DET) {

            if (ttime > Tracer.getTuningLongThreshold()) tunLCalls++;

            if (tunStats.getDetails() == null) {
                tuningExchange(tstamp);
            }

            TraceDetailStats details = tunStats.getDetails();

            if (!details.markCall(mid)) tunStats = tuner.exchange(tunStats);
            if (ttime < LTracer.getMinMethodTime()) details.markDrop(mid);
            if (ttime > Tracer.getTuningLongThreshold()) details.markLCall(mid);
        }
    }

    private void tuningExchange(long tstamp) {

        if (tunStats != null) {
            tunStats.setThreadId(Thread.currentThread().getId());
            tunStats.setCalls(tunCalls);
            tunStats.setDrops(tunDrops);
            tunStats.setErrors(tunErrors);
            tunStats.setLcalls(tunLCalls);
            tunStats.setTstamp(tstamp);
            tunCalls = 0;
            tunDrops = 0;
            tunErrors = 0;
            tunLCalls = 0;
        }

        tunStats = tuner.exchange(tunStats);
        tunLastExchange = tstamp;
    }


    /**
     * Sets minimum trace execution time for currently recorded trace.
     * If there is no trace being recorded just yet, this method will
     * have no effect.
     *
     * @param minimumTraceTime (in nanoseconds)
     */
    public abstract void setMinimumTraceTime(long minimumTraceTime);

    public abstract void markTraceFlags(int traceId, int flag);

    public abstract void markRecordFlags(int flag);

    public abstract boolean isInTrace(int traceId);

    public abstract void traceReturn(long tstamp);

    public abstract void traceEnter(int mid, long tstamp);

    public abstract void traceError(Object e, long tstamp);
}
