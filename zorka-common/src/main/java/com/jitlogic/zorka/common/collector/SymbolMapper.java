package com.jitlogic.zorka.common.collector;

import java.util.Map;

public interface SymbolMapper {

    /**
     * Maps agent symbols to store symbols.
     * @param newSymbols map agentSymId -- symbolName
     * @return map agentSymId -- storeSymId
     */
    Map<Integer,Integer> newSymbols(Map<Integer,String> newSymbols);

    Map<Integer,Integer> newMethods(Map<Integer,int[]> newMethods);

}
