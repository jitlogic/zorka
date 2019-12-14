package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.TraceDataScannerVisitor;

import java.util.HashSet;
import java.util.Set;

public class SymbolsScanningVisitor implements TraceDataScannerVisitor {

    private Set<Integer> symbolIds = new HashSet<Integer>();
    private Set<Integer> methodIds = new HashSet<Integer>();

    public Set<Integer> getSymbolIds() {
        return symbolIds;
    }

    public Set<Integer> getMethodIds() {
        return methodIds;
    }

    @Override
    public int symbolId(int symbolId) {
        symbolIds.add(symbolId);
        return symbolId;
    }

    @Override
    public int methodId(int methodId) {
        methodIds.add(methodId);
        return methodId;
    }
}
