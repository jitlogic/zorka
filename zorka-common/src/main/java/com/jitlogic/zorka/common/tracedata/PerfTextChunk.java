package com.jitlogic.zorka.common.tracedata;

public class PerfTextChunk {

    private long tstamp;
    private String label;
    private byte[] data;

    public PerfTextChunk(String label, byte[] data) {
        this.tstamp = System.currentTimeMillis();
        this.label = label;
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public byte[] getData() {
        return data;
    }

    public long getTstamp() {
        return tstamp;
    }
}
