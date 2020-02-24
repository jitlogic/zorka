/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.collector.SymbolMapper;
import com.jitlogic.zorka.common.collector.SymbolResolver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SymbolRegistry implements SymbolMapper, SymbolResolver {

    /**
     * ID of last symbol added to registry.
     */
    protected AtomicInteger lastSymbolId;

    protected AtomicInteger lastSymbolNew;

    /**
     * Symbol name to ID map
     */
    protected ConcurrentMap<String, Integer> symbolIds;

    /**
     * Symbol ID to name map
     */
    protected ConcurrentMap<Integer, String> symbolNames;

    protected AtomicInteger lastMethodId;

    protected AtomicInteger lastMethodNew;

    protected ConcurrentMap<Long, Integer> methodIds;

    protected ConcurrentMap<Integer, Long> methodDefs;

    public SymbolRegistry() {
        lastSymbolId = new AtomicInteger(0);
        lastSymbolNew = new AtomicInteger(0);
        symbolIds = new ConcurrentHashMap<String, Integer>();
        symbolNames = new ConcurrentHashMap<Integer, String>();

        lastMethodId = new AtomicInteger(0);
        lastMethodNew = new AtomicInteger(0);
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
                id = newid;
            }
        }

        return id;
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

    public boolean hasSymbol(int symbolId) {
        return symbolNames.containsKey(symbolId);
    }

    public String methodDesc(int mid) {
        SymbolicMethod cms = methodDef(mid);
        return cms != null ? symbolName(cms.getClassId()) + "." + symbolName(cms.getMethodId()) + "()" : "<?>";
    }

    public String methodXDesc(int mid) {
        SymbolicMethod cms = methodDef(mid);
        if (cms != null) {
            String pkgName = "";
            String className = symbolName(cms.getClassId());
            int ix = className.lastIndexOf('.');
            if (ix > 0) {
                pkgName = className.substring(0, ix);
                className = className.substring(ix+1);
            }
            return pkgName + "|" + className + "|" +  symbolName(cms.getMethodId()) + "|" + symbolName(cms.getSignatureId());
        }
        return "?";
    }

    /**
     * Adds new symbol to registry (with predefined ID).
     *
     * @param symbolId symbol ID
     * @param symbol   symbol name
     */
    public void putSymbol(int symbolId, String symbol) {

        symbolIds.put(symbol, symbolId);
        symbolNames.put(symbolId, symbol);

        if (symbolId > lastSymbolId.get()) {
            lastSymbolId.set(symbolId);
        }
    }

    private final static long MDEF_MASK = 0x00000000001FFFFFL;


    public int methodId(String className, String methodName, String methodDescription) {
        return methodId(new SymbolicMethod(symbolId(className), symbolId(methodName), symbolId(methodDescription)));
    }


    public int methodId(SymbolicMethod sm) {
        long mdef = sm.mdef();
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


    public SymbolicMethod methodDef(int methodId) {
        Long mdef = methodDefs.get(methodId);
        return mdef != null ? new SymbolicMethod(mdef) : null;
    }


    public boolean hasMethod(int methodId) {
        return methodDefs.containsKey(methodId);
    }


    public void putMethod(int methodId, int className, int methodName, int methodSignature) {
        long mdef = (className & MDEF_MASK)
                | ((methodName & MDEF_MASK) << 21)
                | ((methodSignature & MDEF_MASK) << 42);
        methodIds.put(mdef, methodId);
        methodDefs.put(methodId, mdef);
        if (methodId > lastMethodId.get()) {
            lastMethodId.set(methodId);
        }
    }

    @Override
    public Map<Integer, Integer> newSymbols(Map<Integer, String> newSymbols) {
        Map<Integer,Integer> rslt = new TreeMap<Integer,Integer>();
        for (Map.Entry<Integer,String> e : newSymbols.entrySet()) {
            rslt.put(e.getKey(), symbolId(e.getValue()));
        }
        return rslt;
    }

    @Override
    public Map<Integer, Integer> newMethods(Map<Integer,SymbolicMethod> newMethods) {
        Map<Integer,Integer> rslt = new TreeMap<Integer, Integer>();
        for (Map.Entry<Integer,SymbolicMethod> e : newMethods.entrySet()) {
            rslt.put(e.getKey(), methodId(e.getValue()));
        }
        return rslt;
    }

    @Override
    public Map<Integer, String> resolveSymbols(Set<Integer> symbolIds, int tsnum) {
        Map<Integer,String> rslt = new TreeMap<Integer, String>();
        for (int id : symbolIds) {
            String name = symbolName(id);
            rslt.put(id, name != null ? name : "<?>");
        }
        return rslt;
    }

    @Override
    public Map<Integer,String> resolveMethods(Set<Integer> methodIds, int tsnum) {
        Map<Integer,String> rslt = new TreeMap<Integer,String>();
        for (int mid : methodIds) {
            SymbolicMethod md = methodDef(mid);
            if (md != null) {
                // Poor man's method resolver - for result and argument types use one with asm-util dependency;
                String className = symbolName(md.getClassId());
                String methodName = symbolName(md.getMethodId());
                rslt.put(mid, String.format("%s.%s()", className, methodName));
            } else {
                rslt.put(mid, "<?>");
            }
        }
        return rslt;
    }

    public ConcurrentMap<String, Integer> getSymbolIds() {
        return symbolIds;
    }

    public ConcurrentMap<Integer, String> getSymbolNames() {
        return symbolNames;
    }

    public synchronized void resetNew() {
        lastSymbolNew.set(lastSymbolId.get());
        lastMethodNew.set(lastMethodNew.get());
    }

    public synchronized Map<Integer,String> getNewSymbols() {
        int istart = lastSymbolNew.get(), istop = lastSymbolId.get();
        if (istart >= istop) return Collections.emptyMap();
        Map<Integer,String> rslt = new TreeMap<Integer, String>();
        for (int i = istart+1; i <= istop; i++) {
            rslt.put(i, symbolName(i));
        }
        lastSymbolNew.set(istop);
        return rslt;
    }

    public synchronized Map<Integer,SymbolicMethod> getNewMethods() {
        int istart = lastMethodNew.get(), istop = lastMethodNew.get();
        if (istart >= istop) return Collections.emptyMap();
        Map<Integer,SymbolicMethod> rslt = new TreeMap<Integer, SymbolicMethod>();
        for (int i = istart+1; i <= istop; i++) {
            rslt.put(i, methodDef(i));
        }
        lastMethodNew.set(istop);
        return rslt;
    }
}
