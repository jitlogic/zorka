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

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Tracer implements ZorkaService, ZorkaSubmitter<SymbolicRecord> {

    public final static long DEFAULT_MIN_METHOD_TIME = 250000;

    /** Minimum default method execution time required to attach method to trace. */
    protected static long minMethodTime = DEFAULT_MIN_METHOD_TIME;

    /**Maximum number of records inside trace */
    protected static int maxTraceRecords = 4096;

    protected static int minTraceCalls = 262144;

    /** Tracer tuning disabled. */
    public static final int TUNING_OFF = 0x00;

    /** Summary statistics are collected. */
    public static final int TUNING_SUM = 0x01;

    /** Detail statistics are collected. */
    public static final int TUNING_DET = 0x02;

    /** Default long call threshold for automated tracer tuning: 100ms */
    public static final long TUNING_DEFAULT_LCALL_THRESHOLD = 100 * 1000000L;

    /** Default handler-tuner exchange interval. */
    public static final long TUNING_DEFAULT_EXCH_INTERVAL = 30 * 1000000000L;

    /** Automated tracer tuning mode is disabled by default. */
    protected static int tuningMode = TUNING_OFF;

    /** Threshold above which method call will be considered long-duration. */
    protected static long tuningLongThreshold = TUNING_DEFAULT_LCALL_THRESHOLD;

    /** Interval between handler-tuner exchanges. */
    protected static long tuningExchInterval = TUNING_DEFAULT_EXCH_INTERVAL;

    /** If true, methods instrumented by SPY will also be traced by default. */
    protected boolean traceSpyMethods = true;

    /** Defines which classes and methods should be traced. */
    protected ZtxMatcherSet matcherSet;

    /** Symbol registry containing names of all symbols tracer knows about. */
    protected SymbolRegistry symbolRegistry;

    /** Tracer tuner instance. */
    protected TracerTuner tracerTuner;

    public Tracer(ZtxMatcherSet matcherSet, SymbolRegistry symbolRegistry, TracerTuner tracerTuner) {
        this.matcherSet = matcherSet;
        this.symbolRegistry = symbolRegistry;
        this.tracerTuner = tracerTuner;
    }

    protected AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>> outputs
            = new AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>>(new ArrayList<ZorkaSubmitter<SymbolicRecord>>());



    public abstract TraceHandler getHandler();

    /**
     * Adds new matcher that includes (or excludes) classes and method to be traced.
     *
     * @param matcher spy matcher to be added
     */
    public void include(SpyMatcher matcher) {
        matcherSet.include(matcher);
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

    public boolean isTraceSpyMethods() {
        return traceSpyMethods;
    }


    public void setTraceSpyMethods(boolean traceSpyMethods) {
        this.traceSpyMethods = traceSpyMethods;
    }

    public ZtxMatcherSet getMatcherSet() {
        return matcherSet;
    }

    public static int getTuningMode() {
        return tuningMode;
    }

    public static void setTuningMode(int tuningMode) {
        Tracer.tuningMode = tuningMode;
    }

    public static long getTuningLongThreshold() {
        return tuningLongThreshold;
    }

    public static void setTuningLongThreshold(long tuningLongThreshold) {
        Tracer.tuningLongThreshold = tuningLongThreshold;
    }

    public static long getTuningDefaultExchInterval() {
        return tuningExchInterval;
    }

    public static void setTuningDefaultExchInterval(long tuningDefaultExchInterval) {
        Tracer.tuningExchInterval = tuningDefaultExchInterval;
    }

    public void setMatcherSet(ZtxMatcherSet matcherSet) {
        this.matcherSet = matcherSet;
    }

    public SpyMatcherSet clearMatchers() {
        matcherSet.clear();
        return matcherSet;
    }


    /**
     * Sets output trace event handler tracer will submit completed traces to.
     * Note that submit() method of supplied handler is called from application
     * threads, so it must be thread safe.
     *
     * @param output trace event handler
     */
    public synchronized void addOutput(ZorkaSubmitter<SymbolicRecord> output) {
        List<ZorkaSubmitter<SymbolicRecord>> newOutputs = new ArrayList<ZorkaSubmitter<SymbolicRecord>>(outputs.get());
        newOutputs.add(output);
        outputs.set(newOutputs);
    }

    public List<ZorkaSubmitter<SymbolicRecord>> getOutputs() {
        return Collections.unmodifiableList(outputs.get());
    }

    public TracerTuner getTracerTuner() {
        return tracerTuner;
    }
}
