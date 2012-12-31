package com.jitlogic.zorka.rankproc;

import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.util.ObjectInspector;

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
public class EjbRankItem implements Rankable<Object> {

    private static final ZorkaLog log = ZorkaLogger.getLog(EjbRankItem.class);

    private static final int BY_CALLS = 0;
    private static final int BY_TIME = 1;

    private Object statObj;

    private BucketAggregate byCalls, byTime;


    public EjbRankItem(Object statObj) {
        this.statObj = statObj;

        this.byCalls = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
        this.byTime = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
    }


    public double getAverage(long tstamp, int metric, int average) {
        switch (metric) {
            case BY_CALLS: {
                long dt = byCalls.getDeltaT(average, tstamp);
                return dt > 0 ? (1.0 * byCalls.getDeltaV(average, tstamp) / dt) : 0.0;
            }
            case BY_TIME: {
                long dt = byTime.getDeltaT(average, tstamp);
                return 1.0 * byTime.getDeltaV(average, tstamp) / dt;
            }
            default:
                log.error("Invalid metric passed to getAverage(): " + metric);
                break;
        }

        return 0.0;
    }


    public String[] getMetrics() {
        return new String[] { "CALLS", "TIME" };
    }


    public String[] getAverages() {
        return new String[] { "AVG1", "AVG5", "AVG15" };
    }


    public Object getWrapped() {
        return statObj;
    }


    public String getName() {
        return ""+ObjectInspector.get(statObj,  "name");
    }


    public synchronized void feed(long tstamp) {
        Object count = ObjectInspector.get(statObj, "count");
        Object time = ObjectInspector.get(statObj, "totalTime");

        if (count instanceof Long) {
            byCalls.feed(tstamp, (Long)count);
        }

        if (time instanceof Long) {
            byTime.feed(tstamp, (Long)time);
        }
    }
}

