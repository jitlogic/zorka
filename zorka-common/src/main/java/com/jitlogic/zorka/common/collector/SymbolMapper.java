package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.tracedata.SymbolicMethod;

import java.util.Map;

public interface SymbolMapper {

    /**
     * Maps agent symbols to store symbols.
     * @param newSymbols map agentSymId -- symbolName
     * @return map agentSymId -- storeSymId
     */
    Map<Integer,Integer> newSymbols(Map<Integer,String> newSymbols);

    Map<Integer,Integer> newMethods(Map<Integer,SymbolicMethod> newMethods);

}
