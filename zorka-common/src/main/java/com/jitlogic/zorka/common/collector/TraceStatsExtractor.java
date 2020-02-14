package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.cbor.CborDataReader;
import com.jitlogic.zorka.common.cbor.TraceDataReader;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceStatsExtractor {

    private SymbolResolver resolver;

    public TraceStatsExtractor(SymbolResolver resolver) {
        this.resolver = resolver;
    }

    // TODO dopisać unit test do tego
    public Collection<TraceStatsResult> extract(List<TraceChunkData> chunks) {
        Map<String,TraceStatsResult> rslt = new HashMap<String, TraceStatsResult>();
        for (TraceChunkData c : chunks) {
            TraceStatsExtractingProcessor tsp = new TraceStatsExtractingProcessor();
            byte[] data = ZorkaUtil.gunzip(c.getTraceData());
            new TraceDataReader(new CborDataReader(data), tsp).run();
            Map<Integer,TraceStatsResult> mstats = tsp.getStats();
            Map<Integer,String> mids = resolver.resolveMethods(mstats.keySet(), c.getTsNum());
            for (Map.Entry<Integer,TraceStatsResult> e : mstats.entrySet()) {
                String method = mids.get(e.getKey());
                if (method != null) {
                    TraceStatsResult v = e.getValue();
                    v.setMethod(method);
                    rslt.put(method, v);
                }
            }
        }
        return rslt.values();
    }
}

