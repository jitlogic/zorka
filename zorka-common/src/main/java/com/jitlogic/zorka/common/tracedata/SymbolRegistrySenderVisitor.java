package com.jitlogic.zorka.common.tracedata;

import com.jitlogic.zorka.common.cbor.TraceDataScannerVisitor;
import com.jitlogic.zorka.common.cbor.TraceDataProcessor;
import com.jitlogic.zorka.common.util.BitVector;

public class SymbolRegistrySenderVisitor implements TraceDataScannerVisitor {

    private BitVector symbolsSent = new BitVector();
    private BitVector methodsSent = new BitVector();

    private SymbolRegistry registry;
    private TraceDataProcessor output;

    public SymbolRegistrySenderVisitor(SymbolRegistry registry, TraceDataProcessor output) {
        this.registry = registry;
        this.output = output;
    }

    public void reset() {
        symbolsSent.reset();
        methodsSent.reset();
    }

    @Override
    public int symbolId(int id) {
        if (!symbolsSent.get(id)) {
            String s = registry.symbolName(id);
            if (s != null) {
                output.stringRef(id, s);
                symbolsSent.set(id);
            }
        }
        return id;
    }

    @Override
    public int methodId(int mid) {
        if (!methodsSent.get(mid)) {
            SymbolicMethod md = registry.methodDef(mid);
            if (md != null) {
                symbolId(md.getClassId());
                symbolId(md.getMethodId());
                symbolId(md.getSignatureId());
                output.methodRef(mid, md.getClassId(), md.getMethodId(), md.getSignatureId());
                methodsSent.set(mid);
            }
        }
        return mid;
    }

}
