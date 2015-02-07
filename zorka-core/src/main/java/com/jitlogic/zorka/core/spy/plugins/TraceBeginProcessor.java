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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TraceBuilder;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.Map;

/**
 * Marks beginning of a trace.
 *
 * @author rafal.lewczuk
 */
public class TraceBeginProcessor implements SpyProcessor {


    /**
     * Tracer object.
     */
    private Tracer tracer;


    /**
     * Trace name symbol ID.
     */
    private String traceName;

    private SymbolRegistry symbolRegistry;

    /**
     * Minimum trace execution time.
     */
    private long minimumTraceTime;


    /**
     * Flags set in trace marker.
     */
    private int flags;


    /**
     * Creates new trace begin marking processsor.
     *
     * @param tracer           tracer object
     * @param traceName        trace name (or format string)
     * @param minimumTraceTime minimum trace execution time
     */
    public TraceBeginProcessor(Tracer tracer, String traceName, long minimumTraceTime, int flags, SymbolRegistry symbolRegistry) {
        this.tracer = tracer;
        this.traceName = traceName;
        this.symbolRegistry = symbolRegistry;
        this.minimumTraceTime = minimumTraceTime;
        this.flags = flags;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        TraceBuilder traceBuilder = tracer.getHandler();
        int traceId = symbolRegistry.symbolId(ObjectInspector.substitute(traceName, record));
        traceBuilder.traceBegin(traceId, System.currentTimeMillis(), flags);

        if (minimumTraceTime >= 0) {
            traceBuilder.setMinimumTraceTime(minimumTraceTime);
        }

        return record;
    }
}
