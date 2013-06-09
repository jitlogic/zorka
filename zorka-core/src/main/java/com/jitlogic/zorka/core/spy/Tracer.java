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

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.store.MetricsRegistry;
import com.jitlogic.zorka.core.store.Submittable;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import com.jitlogic.zorka.core.store.SymbolicRecord;
import com.jitlogic.zorka.core.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups all tracer engine components and global settings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class Tracer implements TracerOutput {

    /** Minimum default method execution time required to attach method to trace. */
    private static long minMethodTime = 250000;

    /** Minimum trace execution time required to further process trace */
    private static long minTraceTime = 50000000;

    /** Maximum number of records inside trace */
    private static int maxTraceRecords = 4096;

    private List<ZorkaAsyncThread<Submittable>> outputs = new ArrayList<ZorkaAsyncThread<Submittable>>();

    /** Defines which classes and methods should be traced. */
    private SpyMatcherSet matcherSet;

    /** Symbol registry containing names of all symbols tracer knows about. */
    private SymbolRegistry symbolRegistry;

    private MetricsRegistry metricsRegistry;


    public static long getMinMethodTime() {
        return minMethodTime;
    }


    public static void setMinMethodTime(long methodTime) {
        minMethodTime = methodTime;
    }


    public static long getMinTraceTime() {
        return minTraceTime;
    }


    public static void setMinTraceTime(long traceTime) {
        minTraceTime = traceTime;
    }


    public static int getMaxTraceRecords() {
        return maxTraceRecords;
    }


    public static void setMaxTraceRecords(int traceSize) {
        maxTraceRecords = traceSize;
    }

    /** Thread local serving trace builder objects for application threads */
    private ThreadLocal<TraceEventHandler> localHandlers =
        new ThreadLocal<TraceEventHandler>() {
            public TraceEventHandler initialValue() {
                return new TraceBuilder(Tracer.this, symbolRegistry);
            }
        };


    public Tracer(SpyMatcherSet matcherSet, SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry) {
        this.matcherSet = matcherSet;
        this.symbolRegistry = symbolRegistry;
        this.metricsRegistry = metricsRegistry;
    }

    /**
     * Returns trace even handler receiving events from local application thread.
     *
     * @return trace event handler (trace builder object)
     */
    public TraceEventHandler getHandler() {
        return localHandlers.get();
    }

    /**
     * Adds new matcher that includes (or excludes) classes and method to be traced.
     *
     * @param matcher spy matcher to be added
     */
    public void include(SpyMatcher matcher) {
        matcherSet.include(matcher);
    }



    public void submit(Submittable record) {
        for (ZorkaAsyncThread<Submittable> output : outputs) {
            output.submit(record);
        }
    }


    /**
     * Sets output trace event handler tracer will submit completed traces to.
     * Note that submit() method of supplied handler is called from application
     * threads, so it must be thread safe.
     *
     * @param output trace event handler
     */
    public void addOutput(ZorkaAsyncThread<Submittable> output) {
        outputs.add(output);
    }



    public SpyMatcherSet getMatcherSet() {
        return matcherSet;
    }

}
