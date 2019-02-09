package com.jitlogic.zorka.core.perfmon;

public class ThreadMonitorRankItem {

    private long tid;
    private String name;
    private double avg;

    public ThreadMonitorRankItem(long tid, String name, double avg) {
        this.tid = tid;
        this.name = name;
        this.avg = avg;
    }

    public long getTid() {
        return tid;
    }

    public String getName() {
        return name;
    }

    public double getAvg() {
        return avg;
    }
}
