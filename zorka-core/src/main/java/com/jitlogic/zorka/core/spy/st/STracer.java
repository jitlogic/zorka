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

package com.jitlogic.zorka.core.spy.st;


import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.spy.SpyMatcher;
import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;

/**
 * Groups all tracer engine components and global settings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class STracer extends Tracer {

    /**
     * Minimum default method execution time required to attach method to trace.
     */
    private static long minMethodTime = 250000;

    /**
     * Maximum number of records inside trace
     */
    private static int maxTraceRecords = 4096;

    private static long minTraceTime = 1000000000L;

    /**
     * Symbol registry containing names of all symbols tracer knows about.
     */
    private SymbolRegistry symbolRegistry;

    /**
     * Buffer manager for streaming tracer.
     */
    private STraceBufManager bufManager;

    /**
     * Buffer output for streaming tracer.
     */
    private STraceBufOutput bufOutput;


    /**
     * Thread local serving streaming tracer objects for application threads.
     */
    private ThreadLocal<STraceHandler> handlers =
        new ThreadLocal<STraceHandler>() {
            public STraceHandler initialValue() {
                STraceHandler recorder = new STraceHandler(bufManager, symbolRegistry, bufOutput);
                recorder.setMinimumMethodTime(minMethodTime >> 16);
                return recorder;
            }
        };

    public STracer(SpyMatcherSet matcherSet, SymbolRegistry symbolRegistry, STraceBufManager bufManager) {
        this.matcherSet = matcherSet;
        this.symbolRegistry = symbolRegistry;
        this.bufManager = bufManager;
    }


    public STraceHandler getStHandler() {
        return handlers.get();
    }

    /**
     * Adds new matcher that includes (or excludes) classes and method to be traced.
     *
     * @param matcher spy matcher to be added
     */
    @Override
    public void include(SpyMatcher matcher) {
        matcherSet = matcherSet.include(matcher);
    }

    public SpyMatcherSet clearMatchers() {
        SpyMatcherSet ret = matcherSet;
        matcherSet = new SpyMatcherSet();
        return ret;
    }


    @Override
    public synchronized void shutdown() {
    }


    public SpyMatcherSet getMatcherSet() {
        return matcherSet;
    }

    public void setMatcherSet(SpyMatcherSet matcherSet) {
        this.matcherSet = matcherSet;
    }

    public void setBufOutput(STraceBufOutput bufOutput) {
        this.bufOutput = bufOutput;
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

    public static void setMinTraceTime(long traceTime) { STracer.minTraceTime = traceTime; }

    public static long getMinTraceTime() { return STracer.minTraceTime; }

    @Override
    public TraceHandler getHandler() {
        return null;
    }


    @Override
    public boolean submit(SymbolicRecord item) {
        // TODO not implemented
        return false;
    }
}
