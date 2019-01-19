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

package com.jitlogic.zorka.core.spy.stracer;


import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.spy.tuner.ZtxMatcherSet;

import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_STREAMING_CHUNKED_DEFV;
import static com.jitlogic.zorka.core.AgentConfigProps.TRACER_STREAMING_CHUNKED_PROP;

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

    private boolean streamingEnabled;

    /**
     * Thread local serving streaming tracer objects for application threads.
     */
    private ThreadLocal<STraceHandler> handlers =
        new ThreadLocal<STraceHandler>() {
            public STraceHandler initialValue() {
                return new STraceHandler(streamingEnabled,
                        bufManager, symbolRegistry, tracerTuner, outputs.get().get(0));
            }
        };

    public STracer(ZorkaConfig config, ZtxMatcherSet matcherSet, SymbolRegistry symbolRegistry, TracerTuner tuner, STraceBufManager bufManager) {
        super(matcherSet, symbolRegistry, tuner);
        this.streamingEnabled = config.boolCfg(TRACER_STREAMING_CHUNKED_PROP, TRACER_STREAMING_CHUNKED_DEFV);
        this.symbolRegistry = symbolRegistry;
        this.bufManager = bufManager;
    }


    public STraceHandler getStHandler() {
        return handlers.get();
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
