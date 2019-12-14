package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.cbor.TraceDataScannerVisitor;

import java.util.HashSet;
import java.util.Set;

public class TapSymbolsScanningVisitor implements TraceDataScannerVisitor {

    private Set<Integer> symbols = new HashSet<Integer>();
    private Set<Integer> methods = new HashSet<Integer>();
    private int fuzz;

    public TapSymbolsScanningVisitor(int fuzz) {
        this.fuzz = fuzz;
    }

    public boolean hasSymbol(int symbolId) {
        return symbols.contains(symbolId);
    }

    public boolean hasMethod(int methodId) {
        return methods.contains(methodId);
    }

    @Override
    public int symbolId(int symbolId) {
        symbols.add(symbolId);
        return symbolId + fuzz;
    }

    @Override
    public int methodId(int methodId) {
        methods.add(methodId);
        return methodId + fuzz;
    }
}
