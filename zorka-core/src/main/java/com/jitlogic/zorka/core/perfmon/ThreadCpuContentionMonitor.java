package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.List;

public class ThreadCpuContentionMonitor implements Runnable {

    private Logger log = LoggerFactory.getLogger(ThreadCpuContentionMonitor.class);

    /** Maximum number of threads to put in single dump. */
    private int maxThreads;

    /** Minimum CPU per thread in order to be dumped.  */
    private int minCpuThread;

    /** Minimal total CPU utilization in order to perform thread dumps. */
    private int minCpuTotal;

    /** Maximum stack depth of thread dumps. */
    private int stackDepth;

    /** Output trapper. */
    private ZorkaAsyncThread<String> output;

    /** Rank list used to analyze threads. */
    private final RankList<ThreadRankItem> rankList;

    /** Thread MX bean implements methods useful for obtaining information about running threads. */
    private ThreadMXBean threadMXBean;

    /** MBean server registry (used only to check if obtaining ThreadMXBean is possible) */
    private MBeanServerRegistry mBeanServerRegistry;

    public ThreadCpuContentionMonitor(
            ZorkaAsyncThread<String> output, RankList<ThreadRankItem> rankList, MBeanServerRegistry mBeanServerRegistry,
            int maxThreads, int minCpuThread, int minCpuTotal, int stackDepth) {
        this.output = output;
        this.rankList = rankList;
        this.mBeanServerRegistry = mBeanServerRegistry;
        this.maxThreads = maxThreads;
        this.minCpuThread = minCpuThread;
        this.minCpuTotal = minCpuTotal;
        this.stackDepth = stackDepth;
    }

    private void runCycle() {

        if (log.isDebugEnabled()) {
            log.debug("starting monitoring cycle ...");
        }

        List<ThreadRankItem> lst = rankList.list();

        double[] cpus = new double[maxThreads];
        long t = System.currentTimeMillis();
        int n = Math.min(maxThreads, lst.size());
        double cpuu = 0;

        for (int i = 0; i < n; i++) {
            double cpu = lst.get(i).getAverage(t, 0, 1);
            cpuu += cpu;
            cpus[i] = cpu;
        }

        if (cpuu > minCpuTotal) {
            StringBuilder sb = new StringBuilder();
            sb.append("------------------------------------------------------\n");
            sb.append(new Date() + ": CPU[0.."+ n + "]=" + cpuu + "%\n");

            for (int i = 0; i < maxThreads; i++) {
                if (cpus[i] > minCpuThread) {
                    ThreadInfo ti = threadMXBean.getThreadInfo(lst.get(i).getId(), stackDepth);
                    sb.append("ID=" + ti.getThreadId() + " (" + ti.getThreadName() + "): CPU=" + cpus[i] + "%\n");
                    for (StackTraceElement se : ti.getStackTrace()) {
                        sb.append("  " + se.getClassName() + "." + se.getMethodName() + "() [" +
                                se.getFileName() + ":" + se.getLineNumber() + "]\n");
                    }
                    sb.append("\n");
                }
            }

            sb.append("\n\n");
            output.submit(sb.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("finished monitoring cycle (cpuu=" + cpuu + "%).");
        }
    }

    @Override
    public void run() {
        try {
            if (threadMXBean == null) {
                if (mBeanServerRegistry.lookup("java") != null) {
                    threadMXBean = ManagementFactory.getThreadMXBean();
                } else {
                    return;
                }
            }
            runCycle();
        } catch (Exception e) {
            log.error("Error running ThreadRankStackSampler cycle", e);
        }
    }
}
