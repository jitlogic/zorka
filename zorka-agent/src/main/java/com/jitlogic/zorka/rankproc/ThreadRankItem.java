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

import java.lang.management.ThreadInfo;
import static com.jitlogic.zorka.rankproc.BucketAggregate.*;

public class ThreadRankItem implements Rankable<ThreadInfo> {

    private ThreadInfo threadInfo;

    private final static int BY_CPU = 0;
    private final static int BY_BLOCK = 1;

    private BucketAggregate byCpuTime, byBlockedTime;

    public ThreadRankItem(ThreadInfo threadInfo) {
        this.threadInfo = threadInfo;
        byCpuTime = new BucketAggregate(NS, 30, 2, 5, 3);
        byBlockedTime = new BucketAggregate(NS, 30, 2, 5, 3);
    }


    public synchronized double getAverage(int metric, int average) {
        switch (metric) {
            case BY_CPU:
                return 1000.0 * byCpuTime.getDelta(average) / (byCpuTime.getWindow(average) * MS);
            case BY_BLOCK:
                return 1000.0 * byBlockedTime.getDelta(average) / byBlockedTime.getWindow(average) * MS;
        }
        return 0.0;
    }


    public String[] getMetrics() {
        return new String[] { "CPU", "BLOCK" };
    }


    public String[] getAverages() {
        return new String[] { "10s", "AVG1", "AVG5", "AVG15" };
    }


    public ThreadInfo getWrapped() {
        return threadInfo;
    }


    public String getName() {
        return threadInfo.getThreadName();
    }


    public synchronized void feed(long tstamp, ThreadInfo threadInfo, long threadTime) {

        this.threadInfo = threadInfo; // TODO copy only required values to avoid memory spill

        if (threadTime >= 0) {
            byCpuTime.feed(tstamp, threadTime);
        }

        byBlockedTime.feed(tstamp, threadInfo.getBlockedTime());
    }

}
