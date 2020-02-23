package com.jitlogic.zorka.common.collector;

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.HashMap;
import java.util.Map;

public class TraceChunkSearchQuery {

    private long traceId1, traceId2;
    private long spanId;

    private boolean errorsOnly;
    private boolean spansOnly;

    private String text;

    private Map<String,String> attrmatches = new HashMap<String, String>();

    private long minDuration;
    private long minTstamp = Long.MIN_VALUE, maxTstamp = Long.MAX_VALUE;

    private int limit = 100;
    private int offset;
    private boolean fetchAttrs = true;
    private boolean sortByDuration;

    @Override
    public String toString() {
        return "Q["
            + "tid=" + ZorkaUtil.hex(traceId1, traceId2) + ",sid=" + ZorkaUtil.hex(spanId)
            + ",err=" + errorsOnly + ",spo=" + spansOnly + ",dur=" + minDuration
            + ",off=" + offset + ",lim=" + limit + ",sort=" + sortByDuration
            + ",from=" + minTstamp + ",to=" + maxTstamp
            + ",text='" + text + "'" + ",attr=" + attrmatches + "]";
    }

    public long getTraceId1() {
        return traceId1;
    }

    public void setTraceId1(long traceId1) {
        this.traceId1 = traceId1;
    }

    public long getTraceId2() {
        return traceId2;
    }

    public void setTraceId2(long traceId2) {
        this.traceId2 = traceId2;
    }

    public long getSpanId() {
        return spanId;
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public boolean isErrorsOnly() {
        return errorsOnly;
    }

    public void setErrorsOnly(boolean errorsOnly) {
        this.errorsOnly = errorsOnly;
    }

    public boolean isSpansOnly() {
        return spansOnly;
    }

    public void setSpansOnly(boolean spansOnly) {
        this.spansOnly = spansOnly;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, String> getAttrmatches() {
        return attrmatches;
    }

    public void setAttrmatches(Map<String, String> attrmatches) {
        this.attrmatches = attrmatches;
    }

    public TraceChunkSearchQuery withAttr(String attr, String val) {
        if (val != null) {
            attrmatches.put(attr, val);
        } else {
            attrmatches.remove(attr);
        }
        return this;
    }

    public TraceChunkSearchQuery withTraceId(String traceId) {
        if (traceId == null) {
            throw new NullPointerException("Null traceId passed.");
        } else if (traceId.length() == 32) {
            long[] tids = ZorkaUtil.unhex128(traceId);
            traceId1 = tids[0];
            traceId2 = tids[1];
        } else if (traceId.length() == 16) {
            traceId1 = ZorkaUtil.unhex64(traceId);
            traceId2 = 0;
        } else {
            throw new ZorkaRuntimeException("Invalid traceId: '" + traceId + "'");
        }
        return this;
    }

    public TraceChunkSearchQuery withSpanId(String spanId) {
        this.spanId = ZorkaUtil.unhex64(spanId);
        return this;
    }

    public long getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(long minDuration) {
        this.minDuration = minDuration;
    }

    public long getMinTstamp() {
        return minTstamp;
    }

    public void setMinTstamp(long minTstamp) {
        this.minTstamp = minTstamp;
    }

    public long getMaxTstamp() {
        return maxTstamp;
    }

    public void setMaxTstamp(long maxTstamp) {
        this.maxTstamp = maxTstamp;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isFetchAttrs() {
        return fetchAttrs;
    }

    public void setFetchAttrs(boolean fetchAttrs) {
        this.fetchAttrs = fetchAttrs;
    }

    public boolean isSortByDuration() {
        return sortByDuration;
    }

    public void setSortByDuration(boolean sortByDuration) {
        this.sortByDuration = sortByDuration;
    }
}
