package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;

import java.util.List;

public class ZicoNetkitOutput extends ZorkaAsyncThread<SymbolicRecord> {

    public ZicoNetkitOutput(String name) {
        super("ZORKA-CBOR-OUTPUT");
    }

    @Override
    protected void process(List<SymbolicRecord> obj) {

    }
}
