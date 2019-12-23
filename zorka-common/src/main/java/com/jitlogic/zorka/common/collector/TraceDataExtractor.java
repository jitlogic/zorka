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

        // Extract symbol and method ids
        SymbolsScanningVisitor ssv = new SymbolsScanningVisitor();
        TraceDataScanner tds = new TraceDataScanner(ssv);
        for (TraceChunkData c : chunks) {
            new TraceDataReader(new CborDataReader(c.getTraceData()), tds).run();
        }

        // Resolve symbols and methods
        Map<Integer,String> symbols = resolver.resolveSymbols(ssv.getSymbolIds());
        Map<Integer,String> methods = resolver.resolveMethods(ssv.getMethodIds());

        // Extract trace execution tree
        TraceDataExtractingProcessor tdep = new TraceDataExtractingProcessor(symbols, methods);
        for (TraceChunkData c : chunks) {
            new TraceDataReader(new CborDataReader(c.getTraceData()), tdep).run();
        }

        return tdep.getRoot();
    }

}
