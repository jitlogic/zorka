/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.perfmon;

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Maintains information about running thread in a form suitable for creating rankings.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ThreadRankLister implements Runnable, RankLister<ThreadRankItem> {

    /** MBean server registry (used only to check if obtaining ThreadMXBean is possible) */
    private MBeanServerRegistry mBeanServerRegistry;

    /** Thread MX bean implements methods useful for obtaining information about running threads. */
    private ThreadMXBean threadMXBean;

    /** Map of tracked threads. */
    // TODO use AtomicReference for all such volatile fields
    private volatile Map<Long,ThreadRankItem> threads = new HashMap<Long, ThreadRankItem>();

    /**
     * Creates thread rank lister
     */
    public ThreadRankLister(MBeanServerRegistry registry) {
        this.mBeanServerRegistry = registry;
    }


    @Override
    public List<ThreadRankItem> list() {
        List<ThreadRankItem> lst = new ArrayList<ThreadRankItem>(threads.size()+2)     ;

        for (Map.Entry<Long,ThreadRankItem> e : threads.entrySet()) {
            lst.add(e.getValue());
        }

        return Collections.unmodifiableList(lst);
    }


    /**
     * Return current (raw) list of thread info objects wrapped in ThreadRankInfo type.
     *
     * @return list of threads
     */
    protected List<ThreadRankInfo> rawList() {
        // Platform MBean Server startup might be suspended (eg. for JBoss AS);
        if (threadMXBean == null) {
            if (mBeanServerRegistry.lookup("java") != null) {
                threadMXBean = ManagementFactory.getThreadMXBean();
            } else {
                return new ArrayList<ThreadRankInfo>(1);
            }
        }

        ThreadInfo[] ati = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds());
        List<ThreadRankInfo> lst = new ArrayList<ThreadRankInfo>(ati.length);

        for (ThreadInfo ti : ati) {
            long tid = ti.getThreadId();
            long cpuTime = threadMXBean.getThreadCpuTime(tid);
            lst.add(new ThreadRankInfo(tid, ti.getThreadName(), cpuTime, ti.getBlockedTime()));
        }

        return lst;
    }


    /**
     * Performs single cycle. Invoked from main loop in run() method.
     *
     * @param tstamp current time (milliseconds since Epoch)
     */
    public void runCycle(long tstamp) {
        List<ThreadRankInfo> raw = rawList();
        int sz = Math.max(raw.size(), raw.size());
        Map<Long,ThreadRankItem> newThreads = new HashMap<Long, ThreadRankItem>(sz*2+10, 0.5f);

        synchronized (this) {
            for (ThreadRankInfo threadInfo : raw) {
                if (threadInfo == null) {
                    continue;
                }

                long tid = threadInfo.getId();

                ThreadRankItem threadItem = threads.get(threadInfo.getId());
                if (threadItem == null) {
                    threadItem = new ThreadRankItem(threadInfo);
                }

                threadItem.feed(tstamp, threadInfo);
                newThreads.put(threadInfo.getId(), threadItem);
            }

            threads = newThreads;
        }
    }


    @Override
    public void run() {
        runCycle(System.currentTimeMillis());
    }

}
