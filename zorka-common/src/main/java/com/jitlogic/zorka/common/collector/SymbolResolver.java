package com.jitlogic.zorka.common.collector;

import java.util.Map;
import java.util.Set;

public interface SymbolResolver {

    Map<Integer,String> resolveSymbols(Set<Integer> symbolIds);

    Map<Integer,String> resolveMethods(Set<Integer> methodIds);

}
