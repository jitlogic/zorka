/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.rankproc;

import static com.jitlogic.zorka.rankproc.BucketAggregate.*;

public class ThreadRankItem implements Rankable<ThreadRankInfo> {

    private ThreadRankInfo threadInfo;

    private final static int BY_CPU = 0;
    private final static int BY_BLOCK = 1;

    private BucketAggregate byCpuTime, byBlockedTime;

    public ThreadRankItem(ThreadRankInfo threadInfo) {
        this.threadInfo = threadInfo;
        byCpuTime = new SlidingBucketAggregate(30*SEC, 2, 5, 3);
        byBlockedTime = new SlidingBucketAggregate(30*SEC, 2, 5, 3);
    }


    public synchronized double getAverage(long tstamp, int metric, int average) {

        switch (metric) {
            case BY_CPU:
                return 100.0 * byCpuTime.getDeltaV(tstamp, average) / byCpuTime.getWindow(average);
            case BY_BLOCK:
                return 100.0 * byBlockedTime.getDeltaV(tstamp, average) / byBlockedTime.getWindow(average);
        }

        return 0.0;
    }


    public String[] getMetrics() {
        return new String[] { "CPU", "BLOCK" };
    }


    public String[] getAverages() {
        return new String[] { "30s", "AVG1", "AVG5", "AVG15" };
    }


    public ThreadRankInfo getWrapped() {
        return threadInfo;
    }


    public String getName() {
        return threadInfo.getName();
    }


    /**
     *
     * @param tstamp
     * @param threadInfo
     */
    public synchronized void feed(long tstamp, ThreadRankInfo threadInfo) {

        this.threadInfo = threadInfo; // TODO copy only required values to avoid memory spill

        if (threadInfo.getCpuTime() >= 0) {
            byCpuTime.feed(tstamp * MS, threadInfo.getCpuTime());
        }

        byBlockedTime.feed(tstamp * MS, threadInfo.getBlockedTime());
    }

}
