package com.jitlogic.zorka.common.collector;

import java.util.Map;
import java.util.Set;

public interface SymbolResolver {

    Map<Integer,String> resolveSymbols(Set<Integer> symbolIds, int tsnum);

    Map<Integer,String> resolveMethods(Set<Integer> methodIds, int tsnum);

}
