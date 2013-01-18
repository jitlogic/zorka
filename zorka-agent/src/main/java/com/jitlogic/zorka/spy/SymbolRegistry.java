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

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogConfig;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SymbolRegistry {

    private static final ZorkaLog log = ZorkaLogger.getLog(SymbolRegistry.class);

    private AtomicInteger idCounter = new AtomicInteger(0);

    private ConcurrentHashMap<String,Integer> symbols = new ConcurrentHashMap<String, Integer>();

    private ConcurrentHashMap<Integer,String> idents = new ConcurrentHashMap<Integer,String>();


    public int symbolId(String symbol) {
        Integer id = symbols.get(symbol);

        if (id == null) {
            int newid = idCounter.incrementAndGet();

            if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_SYMBOL_REGISTRY)) {
                log.debug("Adding symbol '" + symbol + "', newid=" + newid);
            }

            id = symbols.putIfAbsent(symbol, newid);
            if (id == null) {
                idents.put(newid, symbol);
                id = newid;
            }
        }

        return id;
    }


    public String symbolName(int symbolId) {
        return idents.get(symbolId);
    }


    public void put(int symbolId, String symbol) {

        if (ZorkaLogConfig.isTracerLevel(ZorkaLogConfig.ZTR_SYMBOL_REGISTRY)) {
            log.debug("Putting symbol '" + symbol + "', newid=" + symbolId);
        }

        symbols.put(symbol, symbolId);
        idents.put(symbolId, symbol);

        // TODO not thread safe !
        if (symbolId > idCounter.get()) {
            idCounter.set(symbolId);
        }
    }


    public int lastId() {
        return idCounter.get();
    }
}
