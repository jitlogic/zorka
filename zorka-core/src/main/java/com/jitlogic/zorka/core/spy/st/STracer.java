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
     * Buffer manager for streaming tracer.
     */
    private STraceBufManager bufManager;

    /**
     * Thread local serving streaming tracer objects for application threads.
     */
    private ThreadLocal<STraceHandler> handlers =
        new ThreadLocal<STraceHandler>() {
            public STraceHandler initialValue() {
                STraceHandler handler = new STraceHandler(bufManager, symbolRegistry, outputs.get().get(0));
                handler.setMinimumMethodTime(minMethodTime >> 16);
                return handler;
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

    @Override
    public synchronized void shutdown() {
    }

    @Override
    public TraceHandler getHandler() {
        return handlers.get();
    }

    @Override
    public boolean submit(SymbolicRecord item) {
        // TODO not implemented
        return false;
    }
}
