package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.util.KVSortingHeap;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadMonitor implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(ThreadMonitor.class);

    // Thread IDs are intentionally kept as integers, so it will fit into KVSortingHeap.
    // Possible collisions are (mostly) harmless and should be very rare.
    private volatile Map<Integer,ThreadMonitorItem> threads = new HashMap<Integer, ThreadMonitorItem>();

    private final MBeanServerRegistry mBeanServerRegistry;

    private volatile ThreadMXBean threadMXBean;

    private int tidSize = 96;
    private int rankLimit = 64;

    public static final long AVG1  = 60000000000L;
    public static final long AVG5  = 300000000000L;
    public static final long AVG15 = 900000000000L;

    private volatile List<ThreadMonitorRankItem> ravg1 = new ArrayList<ThreadMonitorRankItem>();
    private volatile List<ThreadMonitorRankItem> ravg5 = new ArrayList<ThreadMonitorRankItem>();
    private volatile List<ThreadMonitorRankItem> ravg15 = new ArrayList<ThreadMonitorRankItem>();

    private volatile int avi1;
    private volatile int avi5;
    private volatile int avi15;

    public ThreadMonitor(MBeanServerRegistry registry) {
        this.mBeanServerRegistry = registry;
    }


    private void scanThreads(long t) {
        Map<Integer,ThreadMonitorItem> oth = threads;
        Map<Integer,ThreadMonitorItem> nth = new HashMap<Integer, ThreadMonitorItem>();

        for (long tid : threadMXBean.getAllThreadIds()) {
            long tcpu = threadMXBean.getThreadCpuTime(tid);
            ThreadMonitorItem itm = oth.get((int)tid);

            if (itm == null) {
                ThreadInfo ti = threadMXBean.getThreadInfo(tid, 0);
                if (ti != null) {
                    itm = new ThreadMonitorItem(tid, ti.getThreadName(), tidSize);
                }
            }

            if (tcpu >= 0 && itm != null) {
                itm.submit(t, tcpu);
                nth.put((int)tid, itm);
            }
        } // for

        synchronized (this) {
            threads = nth;
        }
    } // scanThreads(t)


    private List<ThreadMonitorRankItem> h2r(Map<Integer,ThreadMonitorItem> oth, KVSortingHeap kvh) {
        List<ThreadMonitorRankItem> rslt = new ArrayList<ThreadMonitorRankItem>(kvh.size());
        for (long kv = kvh.nextkv(); kv != -1; kv = kvh.nextkv()) {
            int tid = KVSortingHeap.id(kv);
            int avi = KVSortingHeap.val(kv);
            ThreadMonitorItem itm = oth.get(tid);
            if (log.isTraceEnabled()) {
                log.trace("h2r: id=" + tid + ", avi=" + avi + " itm=" + itm);
            }
            if (itm != null) rslt.add(new ThreadMonitorRankItem(tid, itm.getName(), avi / 100.0));
        }
        return rslt;
    } // h2r()


    private void genRanks() {
        Map<Integer,ThreadMonitorItem> oth = this.threads;

        KVSortingHeap kvh1  = new KVSortingHeap(rankLimit, false);
        KVSortingHeap kvh5  = new KVSortingHeap(rankLimit, false);
        KVSortingHeap kvh15 = new KVSortingHeap(rankLimit, false);

        int avl1 = 0;
        int avl5 = 0;
        int avl15 = 0;

        for (Map.Entry<Integer,ThreadMonitorItem> e : oth.entrySet()) {
            int xavi1 = e.getValue().avi(AVG1);
            avl1 += xavi1; kvh1.add(e.getKey(), xavi1);
            int xavi5 = e.getValue().avi(AVG5);
            avl5 += xavi5; kvh5.add(e.getKey(), xavi5);
            int xavi15 = e.getValue().avi(AVG15);
            avl15 += xavi15; kvh15.add(e.getKey(), xavi15);
            if (log.isTraceEnabled()) {
                log.trace("genRank: id=" + e.getValue().getId() + " " + e.getValue().getName()
                        + " avi1=" + xavi1 + ", avi5=" + xavi5 + ", avi15=" + avi15);
            }

        }

        log.debug("Generating h2r(avg1) rank ...");
        List<ThreadMonitorRankItem> xxravg1 = h2r(oth, kvh1);
        log.debug("Generating h2r(avg5) rank ...");
        List<ThreadMonitorRankItem> xxravg5 = h2r(oth, kvh5);
        log.debug("Generating h2r(avg15) rank ...");
        List<ThreadMonitorRankItem> xxravg15 = h2r(oth, kvh15);

        synchronized (this) {
            this.ravg1 = xxravg1;
            this.ravg5 = xxravg5;
            this.ravg15 = xxravg15;
            avi1 = avl1;
            avi5 = avl5;
            avi15 = avl15;
        }
    } // getRank()


    @Override
    public void run() {

        if (threadMXBean == null) {
            if (mBeanServerRegistry.lookup("java") != null) {
                synchronized (this) {
                    threadMXBean = ManagementFactory.getThreadMXBean();
                }
            } else {
                return;
            }
        }

        try {
            scanThreads(System.nanoTime());
        } catch (Exception e) {
            log.error("Error scanning threads", e);
        }

        try {
            genRanks();
        } catch (Exception e) {
            log.error("Error generating thread ranking", e);
        }
    } // run()

    public List<ThreadMonitorRankItem> getRavg1() {
        return ravg1;
    }

    public List<ThreadMonitorRankItem> getRavg5() {
        return ravg5;
    }

    public List<ThreadMonitorRankItem> getRavg15() {
        return ravg15;
    }

    public int getAvi1() {
        return avi1;
    }

    public double getAvg1() {
        return avi1 / 100.0;
    }

    public int getAvi5() {
        return avi5;
    }

    public double getAvg5() {
        return avi5 / 100.0;
    }

    public int getAvi15() {
        return avi15;
    }

    public double getAvg15() {
        return avi15 / 100.0;
    }
}
