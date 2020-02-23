package com.jitlogic.zorka.common.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.jitlogic.zorka.common.util.ZorkaUtil.GB;
import static com.jitlogic.zorka.common.util.ZorkaUtil.MB;

/**
 * Simple, memory-only implementation of trace chunk store. Implements
 * standard TraceChunkStore interface along with automatic clean up of
 * excess data and provides ways to search and extract chunks.
 *
 * Memory chunk store is intended to work in small setups and unit tests,
 * so performance is not a priority here. Also concurrency is rather coarse,
 * impacting performance when adding and searching at the same time.
 */
public class MemoryChunkStore implements TraceChunkStore {

    private static Logger log = LoggerFactory.getLogger(MemoryChunkStore.class);

    private long maxSize = 4 * GB;
    private long delSize = 256 * MB;
    private long curSize = 0;

    private volatile List<TraceChunkData> chunks = new ArrayList<TraceChunkData>(1024);

    public MemoryChunkStore() { }

    public MemoryChunkStore(long maxSize, long delSize) {
        this.maxSize = Math.max(maxSize, 512 * MB);
        this.delSize = delSize;
        log.info("Initialized memory store: maxSize={}->{}, delSize={}", maxSize, this.maxSize, delSize);
    }

    private void trim() {
        long t1 = System.currentTimeMillis();
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
        long t2 = System.currentTimeMillis();
        log.info("Trimming chunk store size: {} -> {} (t={}ms)", curSize, csz, (t2-t1));
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
    public synchronized void addAll(List<TraceChunkData> tcds) {
        chunks.addAll(tcds);
        for (TraceChunkData tcd : tcds) {
            tcd.setParent(null);
            curSize += tcd.size();
        }
        while (curSize > maxSize) trim();
    }

    public synchronized Set<String> attrVals(String attrName) {
        Set<String> rslt = new HashSet<String>();
        for (TraceChunkData c : chunks) {
            String s = c.getAttr(attrName);
            if (s != null) rslt.add(s);
        }
        return rslt;
    }

    public synchronized TraceChunkData get(int idx) {
        return chunks.get(idx);
    }

    public synchronized List<TraceChunkData> getChunks() {
        return new ArrayList<TraceChunkData>(chunks);
    }

    /** Returns number of chunks */
    public synchronized int length() {
        return chunks.size();
    }

    public List<TraceChunkData> search(TraceChunkSearchQuery q) {
        List<TraceChunkData> rslt = new ArrayList<TraceChunkData>(1024);

        List<TraceChunkData> cs; // copy array, so we won't block threads adding new chunks

        synchronized (this) {
            cs = new ArrayList<TraceChunkData>(chunks);
        }

        for (TraceChunkData c : cs) {
            // Various small properties
            if (!q.isSpansOnly() && c.getParentId() != 0) continue;
            if ((q.getTraceId1() != 0 || q.getTraceId2() != 0)
                && (q.getTraceId1() != c.getTraceId1() || q.getTraceId2() != c.getTraceId2())) continue;
            if (q.getSpanId() != 0 && q.getSpanId() != c.getSpanId()) continue;
            if (q.isErrorsOnly() && !c.hasError()) continue;
            if (c.getDuration() < q.getMinDuration()) continue;
            if (c.getTstamp() < q.getMinTstamp()) continue;
            if (c.getTstamp() > q.getMaxTstamp()) continue;

            // Full text search
            if (q.getText() != null) {
                String text = q.getText();
                boolean matches = false;
                if ((c.getKlass() + "." + c.getMethod()).contains(text)) {
                    matches = true;
                } else if (c.getAttrs() != null) {
                    for (Map.Entry<String,String> e : c.getAttrs().entrySet()) {
                        if (e.getKey().contains(text) || e.getValue().contains(text)) {
                            matches = true; break;
                        }
                    }
                }
                if (!matches) continue;
            }

            // Attribute matches
            if (!q.getAttrmatches().isEmpty()) {
                boolean matches = true;
                for (Map.Entry<String,String> e : q.getAttrmatches().entrySet()) {
                    if (!c.getAttr(e.getKey()).equals(e.getValue())) {
                        matches = false; break;
                    }
                }
                if (!matches) continue;
            }

            rslt.add(c);
        }

        if (q.getOffset()  >= rslt.size()) return Collections.emptyList();

        if (q.isSortByDuration()) {
            Collections.sort(rslt,
                new Comparator<TraceChunkData>() {
                    @Override
                    public int compare(TraceChunkData o1, TraceChunkData o2) {
                        if (o1.getDuration() < o2.getDuration()) return -1;
                        if (o1.getDuration() > o2.getDuration()) return 1;
                        return 0;
                    }
                });
            return rslt.subList(q.getOffset(), Math.min(rslt.size(), q.getOffset()+q.getLimit()));
        } else {
            return rslt.subList(Math.max(0, rslt.size()-q.getOffset()-q.getLimit()), rslt.size()-q.getOffset());
        }
    }

    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void setDelSize(long delSize) {
        this.delSize = delSize;
    }
}
