package com.jitlogic.zorka.core.perfmon;

/**
 * Tracks CPU usage of
 */
public class ThreadMonitorItem {

    private long id;
    private int size;
    private String name;

    private long[] tstamps;
    private long[] cputime;

    private int start = -1, end = 0;


    /**
     * Creates single thread monitoring aggregate.
     * @param id thread id
     * @param name thread name
     * @param size number of slots
     */
    public ThreadMonitorItem(long id, String name, int size) {
        this.id = id;
        this.size = size;
        this.name = name;
        tstamps = new long[size];
        cputime = new long[size];
    }


    /**
     * Submits measurement sample
     * @param tst timestamp (nanoseconds)
     * @param cpu CPU time (nanoseconds)
     */
    public synchronized void submit(long tst, long cpu) {

        tstamps[end] = tst;
        cputime[end] = cpu;

        if (start == -1) {
            start = 0;
            end = 1;
        } else {
            end = (end+1) % size;
            if (start == end) {
                start = (start+1) % size;
            }
        }
    }


    /**
     * Returns average value
     * @param tspan average calculation time span (nanoseconds)
     * @return average (%CPU)
     */
    public synchronized double avg(long tspan) {
        if (start == -1) return 0.0;

        int x1 = end>0 ? end-1 : size-1;
        int x0 = x1;
        long t1 = tstamps[x1];
        long t0 = t1;

        if (start < end) {
            // Single contigous part (middle of buffers)
            for (int x = x1; x >= start && (t1 - t0) < tspan; x--) {
                t0 = tstamps[x]; x0 = x;
            }
        } else {
            // Part 1: 0...end-1
            for (int x = x1; x >= 0 && (t1 - t0) < tspan; x--) {
                t0 = tstamps[x]; x0 = x;
            }
            // Part 1: start...size-1  (if needed)
            if (t1 - t0 < tspan) {
                for (int x = size-1; x >= start && (t1-t0) < tspan; x--) {
                    t0 = tstamps[x]; x0 = x;
                }
            }
        }

        long t = (cputime[x1]-cputime[x0]);

        return t1>t0 ? (100.0*t/(t1-t0)) : 0.0;
    }

    public int avi(long tspan) {
        return (int)(100 * avg(tspan));
    }


    public long getId() {
        return id;
    }


    public String getName() {
        return name;
    }
}
