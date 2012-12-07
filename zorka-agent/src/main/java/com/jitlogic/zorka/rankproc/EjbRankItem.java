package com.jitlogic.zorka.rankproc;

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

    private final static int BY_CALLS = 0;
    private final static int BY_TIME = 1;

    private Object statObj;
    private ObjectInspector inspector;

    private BucketAggregate byCalls, byTime;


    public EjbRankItem(Object statObj) {
        this.statObj = statObj;
        this.inspector = new ObjectInspector();

        this.byCalls = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
        this.byTime = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
    }


    public double getAverage(long tstamp, int metric, int average) {
        switch (metric) {
            case BY_CALLS: {
                long dt = byCalls.getDeltaT(tstamp, average);
                return dt > 0 ? (1.0 * byCalls.getDeltaV(tstamp, average) / dt) : 0.0;
            }
            case BY_TIME: {
                long dt = byTime.getDeltaT(tstamp, average);
                return 1.0 * byTime.getDeltaV(tstamp, average) / dt;
            }
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
        return ""+inspector.get(statObj,  "name");
    }


    public synchronized void feed(long tstamp) {
        Object count = inspector.get(statObj, "count");
        Object time = inspector.get(statObj, "totalTime");

        if (count instanceof Long) {
            byCalls.feed(tstamp, (Long)count);
        }

        if (time instanceof Long) {
            byTime.feed(tstamp, (Long)time);
        }
    }
}

