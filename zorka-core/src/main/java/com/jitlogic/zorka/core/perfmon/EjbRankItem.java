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

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ObjectInspector;

/**
 * Wraps EJB statistic to be suitable to use with rank lists and maintains average statistics for wrapped object.
 */
public class EjbRankItem implements Rankable<Object> {

    /** Logger. */
    private static final ZorkaLog log = ZorkaLogger.getLog(EjbRankItem.class);

    /** Number of calls metric */
    private static final int BY_CALLS = 0;

    /** Execution time metric */
    private static final int BY_TIME = 1;

    /** Wrapped object */
    private Object statObj;

    /** Call counting aggregate */
    private BucketAggregate byCalls;

    /** Execution time aggregate */
    private BucketAggregate byTime;

    /**
     * Creates new wrapper object
     *
     * @param statObj wrapped EJB statistic object
     */
    public EjbRankItem(Object statObj) {
        this.statObj = statObj;

        this.byCalls = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
        this.byTime = new CircularBucketAggregate(BucketAggregate.SEC, 10, 60, 300, 900);
    }


    @Override
    public double getAverage(long tstamp, int metric, int average) {
        switch (metric) {
            case BY_CALLS: {
                synchronized (byCalls) {
                    long dt = byCalls.getDeltaT(average, tstamp);
                    return dt > 0 ? (1.0 * byCalls.getDeltaV(average, tstamp) / dt) : 0.0;
                }
            }
            case BY_TIME: {
                synchronized (byTime) {
                    long dt = byTime.getDeltaT(average, tstamp);
                    return 1.0 * byTime.getDeltaV(average, tstamp) / dt;
                }
            }
            default:
                log.error(ZorkaLogger.ZAG_ERRORS, "Invalid metric passed to getAverage(): " + metric);
                break;
        }

        return 0.0;
    }


    @Override
    public String[] getMetrics() {
        return new String[] { "CALLS", "TIME" };
    }


    @Override
    public String[] getAverages() {
        return new String[] { "AVG1", "AVG5", "AVG15" };
    }


    @Override
    public Object getWrapped() {
        return statObj;
    }


    @Override
    public String getName() {
        return ""+ObjectInspector.get(statObj,  "name");
    }


    /**
     * Updates averages maintained by this wrapper object.
     *
     * @param tstamp current time
     */
    public void feed(long tstamp) {
        Object count = ObjectInspector.get(statObj, "count");
        Object time = ObjectInspector.get(statObj, "totalTime");

        if (count instanceof Long) {
            synchronized (byCalls) {
                byCalls.feed(tstamp, (Long)count);
            }
        }

        if (time instanceof Long) {
            synchronized (byTime) {
                byTime.feed(tstamp, (Long)time);
            }
        }
    }
}

