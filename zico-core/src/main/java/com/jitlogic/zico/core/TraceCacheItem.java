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
package com.jitlogic.zico.core;

import com.jitlogic.zico.core.model.TraceDetailFilterExpression;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

public class TraceCacheItem {

    private TraceDetailFilterExpression filter;

    private TraceRecord trace;

    private long lastAccess = System.currentTimeMillis();

    private long numAccesses = 0;


    public TraceCacheItem(TraceDetailFilterExpression filter, TraceRecord trace) {
        this.filter = filter;
        this.trace = trace;
    }


    public TraceDetailFilterExpression getFilter() {
        return filter;
    }


    public TraceRecord getTrace() {
        return trace;
    }


    public long getLastAccess() {
        return lastAccess;
    }


    public long getNumAccesses() {
        return numAccesses;
    }

    public TraceRecord get() {
        lastAccess = System.currentTimeMillis();
        numAccesses++;
        return trace;
    }
}

