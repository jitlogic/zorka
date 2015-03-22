/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Groups all tracer engine components and global settings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class Tracer implements ZorkaSubmitter<SymbolicRecord>, ZorkaService {

    /**
     * Minimum default method execution time required to attach method to trace.
     */
    private static long minMethodTime = 250000;

    /**
     * Maximum number of records inside trace
     */
    private static int maxTraceRecords = 4096;


    private AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>> outputs
            = new AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>>(new ArrayList<ZorkaSubmitter<SymbolicRecord>>());

    /**
     * Defines which classes and methods should be traced.
     */
    private SpyMatcherSet matcherSet;

    /**
     * Symbol registry containing names of all symbols tracer knows about.
     */
    private SymbolRegistry symbolRegistry;


    /**
     * If true, methods instrumented by SPY will also be traced by default.
     */
    private boolean traceSpyMethods = true;


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


    public boolean isTraceSpyMethods() {
        return traceSpyMethods;
    }


    public void setTraceSpyMethods(boolean traceSpyMethods) {
        this.traceSpyMethods = traceSpyMethods;
    }


    /**
     * Thread local serving trace builder objects for application threads
     */
    private ThreadLocal<TraceBuilder> localHandlers =
            new ThreadLocal<TraceBuilder>() {
                public TraceBuilder initialValue() {
                    return new TraceBuilder(Tracer.this, symbolRegistry);
                }
            };


    public Tracer(SpyMatcherSet matcherSet, SymbolRegistry symbolRegistry) {
        this.matcherSet = matcherSet;
        this.symbolRegistry = symbolRegistry;
    }


    /**
     * Returns trace even handler receiving events from local application thread.
     *
     * @return trace event handler (trace builder object)
     */
    public TraceBuilder getHandler() {
        return localHandlers.get();
    }


    /**
     * Adds new matcher that includes (or excludes) classes and method to be traced.
     *
     * @param matcher spy matcher to be added
     */
    public void include(SpyMatcher matcher) {
        matcherSet = matcherSet.include(matcher);
    }

    public SpyMatcherSet clearMatchers() {
        SpyMatcherSet ret = matcherSet;
        matcherSet = new SpyMatcherSet();
        return ret;
    }


    @Override
    public boolean submit(SymbolicRecord record) {
        boolean submitted = false;
        for (ZorkaSubmitter<SymbolicRecord> output : outputs.get()) {
            submitted |= output.submit(record);
        }
        return submitted;
    }

    @Override
    public synchronized void shutdown() {
        List<ZorkaSubmitter<SymbolicRecord>> old = outputs.get();
        outputs.set(new ArrayList<ZorkaSubmitter<SymbolicRecord>>());

        for (ZorkaSubmitter<SymbolicRecord> output : old) {
            if (output instanceof ZorkaService) {
                ((ZorkaService)output).shutdown();
            }
        }

        if (old.size() > 0) {
            ZorkaUtil.sleep(100);
        }
    }


    /**
     * Sets output trace event handler tracer will submit completed traces to.
     * Note that submit() method of supplied handler is called from application
     * threads, so it must be thread safe.
     *
     * @param output trace event handler
     */
    public synchronized void addOutput(ZorkaSubmitter<SymbolicRecord> output) {
        List<ZorkaSubmitter<SymbolicRecord>> newOutputs = new ArrayList<ZorkaSubmitter<SymbolicRecord>>();
        newOutputs.addAll(outputs.get());
        newOutputs.add(output);
        outputs.set(newOutputs);
    }


    public SpyMatcherSet getMatcherSet() {
        return matcherSet;
    }


    public void setMatcherSet(SpyMatcherSet matcherSet) {
        this.matcherSet = matcherSet;
    }

}
