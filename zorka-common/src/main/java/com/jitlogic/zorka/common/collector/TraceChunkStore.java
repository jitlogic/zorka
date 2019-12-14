package com.jitlogic.zorka.common.collector;

import java.util.List;

public interface TraceChunkStore {

    void add(TraceChunkData tcd);

    void addAll(List<TraceChunkData> tcds);

}
