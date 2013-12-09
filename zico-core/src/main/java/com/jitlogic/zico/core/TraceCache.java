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


import com.google.inject.Inject;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;


@Singleton
public class TraceCache {

    private int maxTraces;

    private List<TraceCacheItem> cache;

    @Inject
    public TraceCache(ZicoConfig config) {
        this.maxTraces = config.intCfg("trace.cache.size", 5);
        cache = new ArrayList<TraceCacheItem>(maxTraces + 1);
    }


    public synchronized TraceRecord get(TraceDetailFilterExpression filter) {
        for (TraceCacheItem item : cache) {
            if (filter.equals(item.getFilter())) {
                return item.get();
            }
        }
        return null;
    }


    public synchronized void put(TraceDetailFilterExpression filter, TraceRecord trace) {
        cache.add(new TraceCacheItem(filter, trace));

        if (cache.size() > maxTraces) {
            TraceCacheItem remove = cache.get(0);
            for (TraceCacheItem item : cache) {
                if (item.getLastAccess() < remove.getLastAccess()) {
                    remove = item;
                }
            }
            cache.remove(remove);
        }
    }
}
