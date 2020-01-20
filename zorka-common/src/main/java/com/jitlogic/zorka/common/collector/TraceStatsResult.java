package com.jitlogic.zorka.common.collector;

public class TraceStatsResult {

    private int mid;

    private long calls;

    private long recs;

    private long errors;

    private long minDuration = Long.MAX_VALUE;

    private long maxDuration = Long.MIN_VALUE;

    private long sumDuration;

    private String method;

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public void addCalls(long calls) {
        this.calls += calls;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public void addRecs(long recs) {
        this.recs += recs;
    }

    public long getRecs() {
        return recs;
    }

    public void setRecs(long recs) {
        this.recs = recs;
    }

    public void addErrors(long errors) {
        this.errors += errors;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public void addDuration(long duration) {
        if (duration > 0) {
            this.minDuration = Math.min(this.minDuration, duration);
            this.maxDuration = Math.max(this.maxDuration, duration);
            this.sumDuration += duration;
        }
    }

    public long getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(long minDuration) {
        this.minDuration = minDuration;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
    }

    public long getSumDuration() {
        return sumDuration;
    }

    public void setSumDuration(long sumDuration) {
        this.sumDuration = sumDuration;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "TSR(" + method + ",dmin=" + minDuration + ",dmax=" + maxDuration + ",dsum" + sumDuration + ")";
    }
}
