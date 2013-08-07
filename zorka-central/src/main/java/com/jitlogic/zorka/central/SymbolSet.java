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
package com.jitlogic.zorka.central;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SymbolSet implements Closeable {

    private int lastId = 0;
    private Map<Integer,String> symbols;
    private Map<String,Integer> symbolNames = new HashMap<String, Integer>();

    private DB db;

    public SymbolSet(String path) {
        db = DBMaker.newFileDB(new File(path))
                .closeOnJvmShutdown()
                .asyncFlushDelay(10)
                .randomAccessFileEnable()
                .make();
        symbols = db.getHashMap("symbols");

        for (Map.Entry<Integer,String> e : symbols.entrySet()) {
            symbolNames.put(e.getValue(), e.getKey());
            if (e.getKey() > lastId) {
                lastId = e.getKey();
            }
        }
    }


    @Override
    public void close() throws IOException {
        db.close();
    }


    public synchronized void put(int id, String symbol) {
        symbols.put(id, symbol);
        symbolNames.put(symbol, id);
        if (id > lastId) {
            lastId = id;
        }
    }


    public synchronized int get(String symbol) {
        if (symbolNames.containsKey(symbol)) {
            return symbolNames.get(symbol);
        } else {
            int id = ++lastId;
            symbols.put(id, symbol);
            symbolNames.put(symbol, id);
            return id;
        }
    }


    public synchronized String get(int id) {
        return symbols.get(id);
    }
}
