package com.jitlogic.zorka.core.spy.tuner;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.BitVector;

public class CidsMidsZtxReader extends AbstractZtxReader {

    private BitVector cids;
    private BitVector mids;
    private SymbolRegistry registry;

    public CidsMidsZtxReader(SymbolRegistry registry, BitVector cids, BitVector mids) {
        this.cids = cids;
        this.mids = mids;
        this.registry = registry;
    }

    @Override
    public void add(String p, String c, String m, String s) {
        String cl = p + "." + c;
        cids.set(registry.symbolId(cl));
        mids.set(registry.methodId(cl, m, s));
    }
}
