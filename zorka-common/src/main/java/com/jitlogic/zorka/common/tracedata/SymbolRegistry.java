/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import java.util.concurrent.atomic.AtomicInteger;

public class SymbolRegistry {

    /**
     * ID of last symbol added to registry.
     */
    protected AtomicInteger lastSymbolId;

    /**
     * Symbol name to ID map
     */
    protected ConcurrentMap<String, Integer> symbolIds;

    /**
     * Symbol ID to name map
     */
    protected ConcurrentMap<Integer, String> symbolNames;

    protected AtomicInteger lastMethodId;

    protected ConcurrentMap<Long, Integer> methodIds;

    protected ConcurrentMap<Integer, Long> methodDefs;

    public SymbolRegistry() {
        lastSymbolId = new AtomicInteger(0);
        symbolIds = new ConcurrentHashMap<String, Integer>();
        symbolNames = new ConcurrentHashMap<Integer, String>();

        lastMethodId = new AtomicInteger(0);
        methodIds = new ConcurrentHashMap<Long, Integer>();
        methodDefs = new ConcurrentHashMap<Integer, Long>();
    }

    /**
     * Returns ID of named symbol. If symbol hasn't been registered yet,
     * it will be and new ID will be assigned for it.
     *
     * @param symbol symbol name
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
                persist(newid, symbol);
                id = newid;
            }
        }

        return id;
    }

    public int trySymbolId(String symbol) {

        if (symbol == null) {
            return 0;
        }

        Integer sym = symbolIds.get(symbol);
        return sym != null ? sym : 0;
    }

    /**
     * Returns symbol name based on ID or null if no such symbol has been registered.
     *
     * @param symbolId symbol ID
     * @return symbol name
     */
    public String symbolName(int symbolId) {
        if (symbolId == 0) {
            return "<null>";
        }
        String sym = symbolNames.get(symbolId);

        return sym != null ? sym : "<?>";
    }

    public String methodDesc(int mid) {
        int[] cms = methodDef(mid);
        return cms != null ? symbolName(cms[0]) + "." + symbolName(cms[1]) + "()" : "<?>";
    }

    /**
     * Adds new symbol to registry (with predefined ID).
     *
     * @param symbolId symbol ID
     * @param symbol   symbol name
     */
    public void put(int symbolId, String symbol) {

        symbolIds.put(symbol, symbolId);
        symbolNames.put(symbolId, symbol);

        if (symbolId > lastSymbolId.get()) {
            lastSymbolId.set(symbolId);
        }

        persist(symbolId, symbol);
    }

    private final static long MDEF_MASK = 0x00000000001FFFFFL;


    public int methodId(String className, String methodName, String methodDescription) {
        return methodId(symbolId(className), symbolId(methodName), symbolId(methodDescription));
    }


    public int methodId(int className, int methodName, int methodDescription) {
        long mdef = (className & MDEF_MASK)
                | ((methodName & MDEF_MASK) << 21)
                | ((methodDescription & MDEF_MASK) << 42);
        Integer id = methodIds.get(mdef);
        if (id == null) {
            int newid = lastMethodId.incrementAndGet();
            id = methodIds.putIfAbsent(mdef, newid);
            if (id == null) {
                methodDefs.put(newid, mdef);
                id = newid;
            }
        }
        return id;
    }


    public int[] methodDef(int methodId) {
        Long mdef = methodDefs.get(methodId);
        return mdef != null ? new int[]{
                (int)(mdef & MDEF_MASK),
                (int)((mdef >> 21) & MDEF_MASK),
                (int)((mdef >> 42) & MDEF_MASK) }
                : null;
    }


    public void putMethod(int methodId, String className, String methodName, String methodDescription) {
        putMethod(methodId, symbolId(className), symbolId(methodName), symbolId(methodDescription));
    }


    public void putMethod(int methodId, int className, int methodName, int methodDescription) {
        long mdef = (className & MDEF_MASK)
                | ((methodName & MDEF_MASK) << 21)
                | ((methodDescription & MDEF_MASK) << 42);
        methodIds.put(mdef, methodId);
        methodDefs.put(methodId, mdef);
        if (methodId > lastMethodId.get()) {
            lastMethodId.set(methodId);
        }
    }


    protected void persist(int id, String name) {
    }

    public int size() {
        return symbolIds.size();
    }

    public int methodCount() { return methodIds.size(); }


    public int lastSymbolId() {
        return lastSymbolId.get();
    }


    public int lastMethodId() {
        return lastMethodId.get();
    }

}
