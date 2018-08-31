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
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;

public abstract class Tracer implements ZorkaService, ZorkaSubmitter<SymbolicRecord> {

    /**
     * Minimum default method execution time required to attach method to trace.
     */
    protected static long minMethodTime = 250000;

    /**
     * Maximum number of records inside trace
     */
    protected static int maxTraceRecords = 4096;

    protected static int minTraceCalls = 262144;

    /**
     * If true, methods instrumented by SPY will also be traced by default.
     */
    protected boolean traceSpyMethods = true;

    /**
     * Defines which classes and methods should be traced.
     */
    protected SpyMatcherSet matcherSet;


    public abstract TraceHandler getHandler();

    public abstract void include(SpyMatcher matcher);


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

    public SpyMatcherSet getMatcherSet() {
        return matcherSet;
    }


    public void setMatcherSet(SpyMatcherSet matcherSet) {
        this.matcherSet = matcherSet;
    }

    public SpyMatcherSet clearMatchers() {
        SpyMatcherSet ret = matcherSet;
        matcherSet = new SpyMatcherSet();
        return ret;
    }

}
