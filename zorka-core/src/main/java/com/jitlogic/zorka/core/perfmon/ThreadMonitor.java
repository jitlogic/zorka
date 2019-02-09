package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.util.KVSortingHeap;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadMonitor implements Runnable {

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
            if (itm != null) rslt.add(new ThreadMonitorRankItem(tid, itm.getName(), avi / 100.0));
        }
        return rslt;
    } // h2r()


    private void genRanks() {
        Map<Integer,ThreadMonitorItem> oth = this.threads;

        KVSortingHeap kvh1  = new KVSortingHeap(rankLimit, true);
        KVSortingHeap kvh5  = new KVSortingHeap(rankLimit, true);
        KVSortingHeap kvh15 = new KVSortingHeap(rankLimit, true);

        for (Map.Entry<Integer,ThreadMonitorItem> e : oth.entrySet()) {
            kvh1.add(e.getKey(), e.getValue().avi(AVG1));
            kvh5.add(e.getKey(), e.getValue().avi(AVG5));
            kvh15.add(e.getKey(), e.getValue().avi(AVG15));
        }

        synchronized (this) {
            ravg1 = h2r(oth, kvh1);
            ravg5 = h2r(oth, kvh5);
            ravg15 = h2r(oth, kvh15);
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

        scanThreads(System.nanoTime());
        genRanks();
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
}
