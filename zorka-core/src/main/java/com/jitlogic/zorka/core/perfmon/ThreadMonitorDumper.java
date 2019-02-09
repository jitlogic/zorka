package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.List;

public class ThreadMonitorDumper implements Runnable {

    private Logger log = LoggerFactory.getLogger(ThreadMonitorDumper.class);

    /** Maximum number of threads to put in single dump. */
    private int maxThreads;

    /** Minimum CPU per thread in order to be dumped.  */
    private double minCpuThread;

    /** Minimal total CPU utilization in order to perform thread dumps. */
    private double minCpuTotal;

    /** Maximum stack depth of thread dumps. */
    private int stackDepth;


    private ThreadMonitor monitor;
    private ZorkaSubmitter<String> output;

    private final MBeanServerRegistry mBeanServerRegistry;

    private volatile ThreadMXBean threadMXBean;

    public ThreadMonitorDumper(
            MBeanServerRegistry mBeanServerRegistry, ThreadMonitor monitor,
            ZorkaSubmitter<String> output,
            int maxThreads, int minCpuThread, int minCpuTotal, int stackDepth) {
        this.mBeanServerRegistry = mBeanServerRegistry;
        this.monitor = monitor;
        this.output = output;
        this.maxThreads = maxThreads;
        this.minCpuThread = minCpuThread;
        this.minCpuTotal = minCpuTotal;
        this.stackDepth = stackDepth;
    }

    private void dumpThreads() {
        StringBuilder sb = new StringBuilder();
        sb.append("------------------------------------------------------\n");
        sb.append(new Date() + ": CPU=" + monitor.getAvg1() + "%\n\n");
        List<ThreadMonitorRankItem> ravg = monitor.getRavg1();
        for (int i = 0; i < Math.min(ravg.size(), maxThreads); i++) {
            ThreadMonitorRankItem itm = ravg.get(i);
            if (log.isTraceEnabled()) {
                log.trace("dumpThreads: item id=" + itm.getTid() + ", avg=" + itm.getAvg() + ", min=" + minCpuThread);;
            }
            if (itm.getAvg() > minCpuThread) {
                ThreadInfo ti = threadMXBean.getThreadInfo(itm.getTid(), stackDepth);
                if (ti != null) {
                    sb.append("ID=" + ti.getThreadId() + " (" + ti.getThreadName() + "): CPU=" + itm.getAvg() + "%\n");
                    for (StackTraceElement se : ti.getStackTrace()) {
                        sb.append("  " + se.getClassName() + "." + se.getMethodName() + "() [" +
                                se.getFileName() + ":" + se.getLineNumber() + "]\n");
                    }
                    sb.append("\n");
                }
            }
        }
        output.submit(sb.toString());
    }

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

        if (monitor.getAvg1() > minCpuTotal) {
            log.debug("Dumping threads ...");
            dumpThreads();
        }
    }

}
