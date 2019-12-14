package com.jitlogic.zorka.common.collector;

import java.util.ArrayList;
import java.util.List;

public class MemoryChunkStore implements TraceChunkStore {

    private List<TraceChunkData> chunks = new ArrayList<TraceChunkData>(512);


    @Override
    public void add(TraceChunkData tcd) {
        chunks.add(tcd);
    }

    @Override
    public void addAll(List<TraceChunkData> tcds) {
        chunks.addAll(tcds);
    }

    public TraceChunkData get(int idx) {
        return chunks.get(idx);
    }

    public int size() {
        return chunks.size();
    }
}
