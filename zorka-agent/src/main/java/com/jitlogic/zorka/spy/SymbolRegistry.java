/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SymbolRegistry {

    private AtomicInteger idCounter = new AtomicInteger(0);

    private ConcurrentHashMap<String,Integer> symbols = new ConcurrentHashMap<String, Integer>();

    private ConcurrentHashMap<Integer,String> idents = new ConcurrentHashMap<Integer,String>();


    public int symbolId(String symbol) {
        Integer id = symbols.get(symbol);

        if (id == null) {
            int newid = idCounter.incrementAndGet();
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


    public int lastId() {
        return idCounter.get();
    }
}
