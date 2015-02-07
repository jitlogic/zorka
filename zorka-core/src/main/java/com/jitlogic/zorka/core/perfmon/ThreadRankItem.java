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

import static com.jitlogic.zorka.core.perfmon.BucketAggregate.*;

/**
 * Tracks a thread and retains information in form suitable for rank lists.
 */
public class ThreadRankItem implements Rankable<ThreadRankInfo> {

    /**
     * thread info (assigned every time thread list is refreshed)
     */
    private volatile ThreadRankInfo threadInfo;

    /**
     * by cpu time metric
     */
    private final static int BY_CPU = 0;

    /**
     * by blocked time metric
     */
    private final static int BY_BLOCK = 1;

    /**
     * Maintains CPU time averages for thread
     */
    private BucketAggregate byCpuTime;

    /**
     * Maintains blocked time averages for thread
     */
    private BucketAggregate byBlockedTime;

    /**
     * Creates new thread rank item
     *
     * @param threadInfo thread information this item will wrap
     */
    public ThreadRankItem(ThreadRankInfo threadInfo) {
        this.threadInfo = threadInfo;
        byCpuTime = new CircularBucketAggregate(SEC, 30, 60, 300, 900);
        byBlockedTime = new CircularBucketAggregate(SEC, 30, 60, 300, 900);
    }


    @Override
    public synchronized double getAverage(long tstamp, int metric, int average) {

        switch (metric) {
            case BY_CPU:
                return 100.0 * byCpuTime.getDeltaV(average, tstamp) / byCpuTime.getWindow(average);
            case BY_BLOCK:
                return 100.0 * byBlockedTime.getDeltaV(average, tstamp) / byBlockedTime.getWindow(average);
        }

        return 0.0;
    }

    @Override
    public String[] getMetrics() {
        return new String[]{"CPU", "BLOCK"};
    }

    @Override
    public String[] getAverages() {
        return new String[]{"30s", "AVG1", "AVG5", "AVG15"};
    }

    @Override
    public ThreadRankInfo getWrapped() {
        return threadInfo;
    }

    @Override
    public String getName() {
        return threadInfo.getName();
    }


    /**
     * This method is used by thread rank lister to update information about tracked thread.
     *
     * @param tstamp     time stamp
     * @param threadInfo thread info object (as from ThreadMXBean)
     */
    public void feed(long tstamp, ThreadRankInfo threadInfo) {

        this.threadInfo = threadInfo; // TODO copy only required values to avoid memory spill

        if (threadInfo.getCpuTime() >= 0) {
            synchronized (byCpuTime) {
                byCpuTime.feed(tstamp * MS, threadInfo.getCpuTime());
            }
        }

        synchronized (byBlockedTime) {
            byBlockedTime.feed(tstamp * MS, threadInfo.getBlockedTime());
        }
    }

}
