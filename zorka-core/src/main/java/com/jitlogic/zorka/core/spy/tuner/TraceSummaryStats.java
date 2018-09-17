package com.jitlogic.zorka.core.spy.tuner;

/**
 * Summary stats data is periodically passed to tracer tuner.
 */
public class TraceSummaryStats {

    private long threadId;
    private long tstamp;
    private long calls;
    private long drops;
    private long errors;
    private long lcalls;

    private TraceDetailStats details;

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getTstamp() {
        return tstamp;
    }

    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public long getDrops() {
        return drops;
    }

    public void setDrops(long drops) {
        this.drops = drops;
    }

    public long getErrors() {
        return errors;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public long getLcalls() {
        return lcalls;
    }

    public void setLcalls(long lcalls) {
        this.lcalls = lcalls;
    }

    public TraceDetailStats getDetails() {
        return details;
    }

    public void setDetails(TraceDetailStats details) {
        this.details = details;
    }

    public void clear() {
        tstamp = calls = drops = errors = lcalls = 0;
    }
}
