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

package com.jitlogic.zorka.core.spy.ltracer;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups all tracer engine components and global settings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LTracer extends Tracer {

    /**
     * Thread local serving trace builder objects for application threads
     */
    private ThreadLocal<LTraceHandler> localHandlers =
            new ThreadLocal<LTraceHandler>() {
                public LTraceHandler initialValue() {
                    return new LTraceHandler(LTracer.this, symbolRegistry, tracerTuner);
                }
            };


    public LTracer(ZtxMatcherSet matcherSet, SymbolRegistry symbolRegistry, TracerTuner tuner) {
        super(matcherSet, symbolRegistry, tuner);
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




}
