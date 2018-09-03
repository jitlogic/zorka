package com.jitlogic.zorka.core.test.spy.support.cbor;

public class STBeg {

    private int traceId;
    private String traceName;
    private long traceClock;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("B(");
        sb.append(traceName);
        sb.append(", ");
        sb.append(traceClock);
        sb.append(")");

        return sb.toString();
    }

    public int getTraceId() {
        return traceId;
    }

    public void setTraceId(int traceId) {
        this.traceId = traceId;
    }

    public String getTraceName() {
        return traceName;
    }

    public void setTraceName(String traceName) {
        this.traceName = traceName;
    }

    public long getTraceClock() {
        return traceClock;
    }

    public void setTraceClock(long traceClock) {
        this.traceClock = traceClock;
    }

}
