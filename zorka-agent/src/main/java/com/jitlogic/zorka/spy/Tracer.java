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

public class Tracer {

    private long methodTime = 250000;

    /** Defines which classes and methods should be traced. */
    private SpyMatcherSet matcherSet = new SpyMatcherSet();

    /** Symbol registry for tracer */
    private SymbolRegistry symbolRegistry = new SymbolRegistry();

    /** Output handler is initially set to null implementation. */
    private ZorkaAsyncThread<TraceElement> output;

    private ThreadLocal<TraceEventHandler> localHandlers =
        new ThreadLocal<TraceEventHandler>() {
            public TraceEventHandler initialValue() {
                return new TraceBuilder(output, methodTime);
            }
        };


    public TraceEventHandler getHandler() {
        return localHandlers.get();
    }


    public SymbolRegistry getSymbolRegistry() {
        return symbolRegistry;
    }


    public void include(SpyMatcher matcher) {
        matcherSet.include(matcher);
    }


    public void setOutput(ZorkaAsyncThread<TraceElement> output) {
        this.output = output;
    }


    public void setMethodTime(long methodTime) {
        this.methodTime = methodTime;
    }

    public SpyMatcherSet getMatcherSet() {
        return matcherSet;
    }

}
