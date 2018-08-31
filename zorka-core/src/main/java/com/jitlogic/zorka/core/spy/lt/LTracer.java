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

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyMatcher;
import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Groups all tracer engine components and global settings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LTracer extends Tracer {


    private AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>> outputs
            = new AtomicReference<List<ZorkaSubmitter<SymbolicRecord>>>(new ArrayList<ZorkaSubmitter<SymbolicRecord>>());


    /**
     * Symbol registry containing names of all symbols tracer knows about.
     */
    private SymbolRegistry symbolRegistry;


    /**
     * Thread local serving trace builder objects for application threads
     */
    private ThreadLocal<LTraceHandler> localHandlers =
            new ThreadLocal<LTraceHandler>() {
                public LTraceHandler initialValue() {
                    return new LTraceHandler(LTracer.this, symbolRegistry);
                }
            };


    public LTracer(SpyMatcherSet matcherSet, SymbolRegistry symbolRegistry) {
        this.matcherSet = matcherSet;
        this.symbolRegistry = symbolRegistry;
    }


    /**
     * Returns trace even handler receiving events from local application thread.
     *
     * @return trace event handler (trace builder object)
     */
    public TraceHandler getHandler() {
        return localHandlers.get();
    }

    public LTraceHandler getLtHandler() {
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
        List<ZorkaSubmitter<SymbolicRecord>> newOutputs = new ArrayList<ZorkaSubmitter<SymbolicRecord>>(outputs.get());
        newOutputs.add(output);
        outputs.set(newOutputs);
    }

    public List<ZorkaSubmitter<SymbolicRecord>> getOutputs() {
        return Collections.unmodifiableList(outputs.get());
    }


}
