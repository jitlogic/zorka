package com.jitlogic.zorka.core.spy.tuner;

public class RankItem {

    private int mid;
    private int rank;
    private long calls;
    private long drops;

    public RankItem(int mid, int rank, long calls, long drops) {
        this.mid = mid;
        this.rank = rank;
        this.calls = calls;
        this.drops = drops;
    }

    public int getMid() {
        return mid;
    }

    public int getRank() {
        return rank;
    }

    public long getCalls() {
        return calls;
    }

    public long getDrops() {
        return drops;
    }

    @Override
    public String toString() {
        return "(m=" + mid + ",r=" + rank + ",c=" + calls + ",d=" + drops + ")";
    }
}
