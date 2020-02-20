/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.ltracer;

import com.jitlogic.zorka.common.tracedata.DTraceContext;
import com.jitlogic.zorka.core.spy.tuner.TraceTuningStats;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TraceHandler {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /** Default long call threshold for automated tracer tuning: 10ms */
    public static final long TUNING_DEFAULT_LCALL_THRESHOLD = 10000000L;

    /** Default handler-tuner exchange interval. */
    public static final long TUNING_DEFAULT_EXCH_INTERVAL = 30 * 1000000000L;

    /** */
    public static final long TUNING_EXCHANGE_CALLS_DEFV = 1048576;

    protected static boolean tuningEnabled = false;

    /** Threshold above which method call will be considered long-duration. */
    protected static long tuningLongThreshold = TUNING_DEFAULT_LCALL_THRESHOLD;

    /** Interval between handler-tuner exchanges. */
    protected static long tuningExchInterval = TUNING_DEFAULT_EXCH_INTERVAL;


    /** Minimum number of calls required to initiate tuning stats exchange */
    private static long tuningExchangeMinCalls = TUNING_EXCHANGE_CALLS_DEFV;

    /** Default: ~0.25ms */
    public final static long DEFAULT_MIN_METHOD_TIME = 262144;

    /** Minimum default method execution time required to attach method to trace. */
    protected static long minMethodTime = DEFAULT_MIN_METHOD_TIME;

    protected static int maxAttrLen = 8192;

    /** Maximum number of records inside trace */
    protected static int maxTraceRecords = 4096;

    /** Number of registered trace calls that will force trace submission regardless of execution time. */
    protected static int minTraceCalls = 262144;


    protected boolean disabled;

    protected long tunCalls = 0;

    protected TracerTuner tuner;
    protected TraceTuningStats tunStats = null;

    protected long tunLastExchange = 0;

    public static final int LONG_PENALTY = -1024;
    public static final int ERROR_PENALTY = -256;

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

        if (tunStats == null ||
                (tstamp > tunLastExchange + tuningExchInterval && tunCalls > tuningExchangeMinCalls)) {
            tuningExchange(tstamp);
        }

        tunCalls++;

        if (tuningEnabled) {


            if (ttime < minMethodTime) {
                if (!tunStats.markRank(mid, 1)) {
                    tuningExchange(tstamp);
                }
            } else if (ttime > tuningLongThreshold) {
                tunStats.markRank(mid, -1 * (int)(ttime >>> 18));
            }

        }
    }

    private void tuningExchange(long tstamp) {

        if (tunStats != null) {
            tunStats.setThreadId(Thread.currentThread().getId());
            tunStats.setCalls(tunCalls);
            tunStats.setTstamp(tstamp);
            tunCalls = 0;
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

    public abstract DTraceContext getDTraceState();

    public abstract DTraceContext parentDTraceState();

    public abstract void setDTraceState(DTraceContext state);

    public static long getTuningExchangeMinCalls() {
        return tuningExchangeMinCalls;
    }

    public static void setTuningExchangeMinCalls(long tuningExchangeMinCalls) {
        TraceHandler.tuningExchangeMinCalls = tuningExchangeMinCalls;
    }

    public static boolean isTuningEnabled() {
        return tuningEnabled;
    }

    public static void setTuningEnabled(boolean tuningEnabled) {
        TraceHandler.tuningEnabled = tuningEnabled;
    }

    public static long getTuningLongThreshold() {
        return tuningLongThreshold;
    }

    public static void setTuningLongThreshold(long tuningLongThreshold) {
        TraceHandler.tuningLongThreshold = tuningLongThreshold;
    }

    public static long getTuningDefaultExchInterval() {
        return tuningExchInterval;
    }

    public static void setTuningDefaultExchInterval(long tuningDefaultExchInterval) {
        TraceHandler.tuningExchInterval = tuningDefaultExchInterval;
    }

    public static long getMinMethodTime() {
        return minMethodTime;
    }


    public static void setMinMethodTime(long methodTime) {
        minMethodTime = methodTime;
    }


    public static int getMaxTraceRecords() {
        return maxTraceRecords;
    }


    public static void setMaxTraceRecords(int traceSize) {
        maxTraceRecords = traceSize;
    }


    public static int getMinTraceCalls() {
        return minTraceCalls;
    }

    public static void setMinTraceCalls(int traceCalls) {
        minTraceCalls = traceCalls;
    }


}
