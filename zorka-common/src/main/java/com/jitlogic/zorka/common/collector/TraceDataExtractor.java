package com.jitlogic.zorka.common.collector;


import com.jitlogic.zorka.common.cbor.CborDataReader;
import com.jitlogic.zorka.common.cbor.TraceDataReader;
import com.jitlogic.zorka.common.cbor.TraceDataScanner;

import java.util.List;
import java.util.Map;

/** Extracts trace data from given set of chunks. */
public class TraceDataExtractor {

    private SymbolResolver resolver;

    public TraceDataExtractor(SymbolResolver resolver) {
        this.resolver = resolver;
    }

    public TraceDataResult extract(List<TraceChunkData> chunks) {

        TraceDataExtractingProcessor tdep = new TraceDataExtractingProcessor();

        // Extract symbol and method ids
        for (TraceChunkData c : chunks) {
            SymbolsScanningVisitor ssv = new SymbolsScanningVisitor();
            TraceDataScanner tds = new TraceDataScanner(ssv);
            new TraceDataReader(new CborDataReader(c.getTraceData()), tds).run();
            Map<Integer,String> symbols = resolver.resolveSymbols(ssv.getSymbolIds(), c.getTsNum());
            Map<Integer,String> methods = resolver.resolveMethods(ssv.getMethodIds(), c.getTsNum());
            tdep.setSymbolMaps(symbols, methods);
            new TraceDataReader(new CborDataReader(c.getTraceData()), tdep).run();
        }

        return tdep.getRoot();
    }

}
