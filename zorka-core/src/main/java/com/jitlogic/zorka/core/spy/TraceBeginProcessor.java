/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
    private int traceId;


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
     * @param traceId          trace id (symbol)
     * @param minimumTraceTime minimum trace execution time
     */
    public TraceBeginProcessor(Tracer tracer, int traceId, long minimumTraceTime, int flags) {
        this.tracer = tracer;
        this.traceId = traceId;
        this.minimumTraceTime = minimumTraceTime;
        this.flags = flags;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        TraceBuilder traceBuilder = tracer.getHandler();
        traceBuilder.traceBegin(traceId, System.currentTimeMillis(), flags);

        if (minimumTraceTime >= 0) {
            traceBuilder.setMinimumTraceTime(minimumTraceTime);
        }

        return record;
    }
}
