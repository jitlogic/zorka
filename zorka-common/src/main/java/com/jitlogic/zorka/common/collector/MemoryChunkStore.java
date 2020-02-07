package com.jitlogic.zorka.common.collector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jitlogic.zorka.common.util.ZorkaUtil.GB;
import static com.jitlogic.zorka.common.util.ZorkaUtil.MB;

public class MemoryChunkStore implements TraceChunkStore {

    private long maxSize = 4 * GB;
    private long delSize = 256 * MB;
    private long curSize = 0;

    private volatile List<TraceChunkData> chunks = new ArrayList<TraceChunkData>(1024);

    public MemoryChunkStore() { }

    public MemoryChunkStore(long maxSize) {
        this.maxSize = maxSize;
    }

    private void trim() {
        int sz = 0, idx = -1;
        for (int i = 0; i < chunks.size() && sz < delSize; i++) {
            sz += chunks.get(i).size();
            idx = i;
        }
        if (idx >= 0 && idx < chunks.size()) {
            synchronized (this) {
                chunks = new ArrayList<TraceChunkData>(chunks.subList(idx + 1, chunks.size()));
            }
        }
        long csz = 0;
        for (TraceChunkData chunk : chunks) csz += chunk.size();
        curSize = csz;
    }

    @Override
    public synchronized void add(TraceChunkData tcd) {
        tcd.setParent(null); // detach parent, so it won't affect memory utilization / GC
        chunks.add(tcd);
        curSize += tcd.size();
        while (curSize > maxSize) trim();
    }

    @Override
    public void addAll(List<TraceChunkData> tcds) {
        chunks.addAll(tcds);
        for (TraceChunkData tcd : tcds) {
            tcd.setParent(null);
            curSize += tcd.size();
        }
        while (curSize > maxSize) trim();
    }

    public Set<String> attrVals(String attrName) {
        Set<String> rslt = new HashSet<String>();
        for (TraceChunkData c : chunks) {
            String s = c.getAttr(attrName);
            if (s != null) rslt.add(s);
        }
        return rslt;
    }

    public TraceChunkData get(int idx) {
        return chunks.get(idx);
    }

    public List<TraceChunkData> getChunks() {
        return chunks;
    }

    /** Returns number of chunks */
    public int length() {
        return chunks.size();
    }
}