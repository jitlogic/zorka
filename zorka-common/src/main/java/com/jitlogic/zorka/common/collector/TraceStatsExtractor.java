package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.CborDataReader;
import com.jitlogic.zorka.common.cbor.TraceDataReader;

import java.util.Collection;
import java.util.List;

public class TraceStatsExtractor {

    public Collection<TraceStatsResult> extract(List<TraceChunkData> chunks) {
        TraceStatsExtractingProcessor tsp = new TraceStatsExtractingProcessor();
        for (TraceChunkData c : chunks) {
            new TraceDataReader(new CborDataReader(c.getTraceData()), tsp).run();
        }
        return tsp.getStats().values();
    }
}

