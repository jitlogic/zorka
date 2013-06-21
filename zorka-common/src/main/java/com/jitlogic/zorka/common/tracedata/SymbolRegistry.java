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

package com.jitlogic.zorka.common.tracedata;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SymbolRegistry {

    /** ID of last symbol added to registry. */
    private AtomicInteger lastSymbolId;

    /** Symbol name to ID map */
    private ConcurrentMap<String,Integer> symbolIds;

    /** Symbol ID to name map */
    private ConcurrentMap<Integer,String> symbolNames;

    public SymbolRegistry() {
        lastSymbolId  = new AtomicInteger(0);
        symbolIds = new ConcurrentHashMap<String, Integer>();
        symbolNames = new ConcurrentHashMap<Integer, String>();
    }

    /**
     * Returns ID of named symbol. If symbol hasn't been registered yet,
     * it will be and new ID will be assigned for it.
     *
     * @param symbol symbol name
     *
     * @return symbol ID (integer)
     */
    public int symbolId(String symbol) {

        if (symbol == null) {
            return 0;
        }

        Integer id = symbolIds.get(symbol);

        if (id == null) {
            int newid = lastSymbolId.incrementAndGet();

            id = symbolIds.putIfAbsent(symbol, newid);
            if (id == null) {
                symbolNames.put(newid, symbol);
                id = newid;
            }
        }

        return id;
    }


    /**
     * Returns symbol name based on ID or null if no such symbol has been registered.
     *
     * @param symbolId symbol ID
     *
     * @return symbol name
     */
    public String symbolName(int symbolId) {
        if (symbolId == 0) {
            return "<null>";
        }
        String sym = symbolNames.get(symbolId);

        return sym != null ? sym : "<?>";
    }


    /**
     * Adds new symbol to registry (with predefined ID).
     *
     * @param symbolId symbol ID
     *
     * @param symbol symbol name
     */
    public void put(int symbolId, String symbol) {

        symbolIds.put(symbol, symbolId);
        symbolNames.put(symbolId, symbol);

        // TODO not thread safe !
        if (symbolId > lastSymbolId.get()) {
            lastSymbolId.set(symbolId);
        }
    }

    public int size() {
        return symbolIds.size();
    }

}
