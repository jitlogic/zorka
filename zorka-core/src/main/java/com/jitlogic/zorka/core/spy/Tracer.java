/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Tracer implements ZorkaService, ZorkaSubmitter<SymbolicRecord> {

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


    public boolean isTraceSpyMethods() {
        return traceSpyMethods;
    }


    public void setTraceSpyMethods(boolean traceSpyMethods) {
        this.traceSpyMethods = traceSpyMethods;
    }

    public ZtxMatcherSet getMatcherSet() {
        return matcherSet;
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
