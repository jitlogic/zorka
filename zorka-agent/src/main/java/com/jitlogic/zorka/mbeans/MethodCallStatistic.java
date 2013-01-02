/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.mbeans;

import static com.jitlogic.zorka.rankproc.BucketAggregate.MS;

import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.rankproc.BucketAggregate;
import com.jitlogic.zorka.rankproc.CircularBucketAggregate;
import com.jitlogic.zorka.rankproc.Rankable;

import java.util.Date;

/**
 * Represents statistics of calls to a single method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MethodCallStatistic implements ZorkaStat, Rankable<MethodCallStatistic> {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(MethodCallStatistic.class);

    /** Number of calls metric ID */
    public static final int CALLS_STAT = 0;

    /** Execution times metric ID */
    public static final int TIMES_STAT = 1;

    /** Number of errors metric ID */
    public static final int ERROR_STAT = 2;

    /** Method name */
	private String name;

    /** Aggregates */
    private BucketAggregate calls, errors, time;

    /**
     * Creates statistic maintaining standard AVG1, AVG5 and AVG15 aggregates.
     * This method assumes that time ticks passed to aggregation methods are in nanoseconds.
     *
     * @param name method name
     *
     * @return instance of method statistic object
     */
    public static MethodCallStatistic newStatAvg15(String name) {
        return new MethodCallStatistic(name, BucketAggregate.SEC, 10, 60, 300, 900);
    }


    /**
     * Standard constructor.
     *
     * @param name statistic name
     *
     * @param base base period (in units passed via tstamp argument, eg. nanoseconds)
     *
     * @param res aggregate resolution
     *
     * @param stages aggregate periods to maintain
     */
    public MethodCallStatistic(String name, long base, int res, int...stages) {
        this.name = name;
        this.calls = new CircularBucketAggregate(base, res, stages);
        this.errors = new CircularBucketAggregate(base, res, stages);
        this.time = new CircularBucketAggregate(base, res, stages);
    }


    /**
     * Returns total aggregated value (sum of all submissions) for given metric
     *
     * @param metric metric to be retrieved
     *
     * @return total aggregated value
     */
    public synchronized long getTotal(int metric) {
        switch (metric) {
            case CALLS_STAT:
                return calls.getTotal();
            case ERROR_STAT:
                return errors.getTotal();
            case TIMES_STAT:
                return time.getTotal() / MS;
            default:
                log.error("Invalid metric passed to getTotal(): " + metric);
                break;
        }

        return -1;
    }


    @Override
    public synchronized double getAverage(long tstamp, int metric, int average) {
        switch (metric) {
            case CALLS_STAT: {
                long delta = calls.getDeltaV(average, tstamp);
                return 1.0 * delta / (calls.getWindow(average) * MS);
            }
            case ERROR_STAT: {
                long delta = errors.getDeltaV(average, tstamp);
                return 1.0 * delta / (errors.getWindow(average) * MS);
            }
            case TIMES_STAT: {
                long dc = calls.getDeltaV(average, tstamp), dt = time.getDeltaV(average, tstamp);
                return dc == 0 ? 0.0 : 1.0 * dt / (dc * MS);
            }
            default:
                log.error("Invalid metric passed to getAverage(): " + metric);
                break;
        }
        return 0.0;
    }


    @Override
    public String[] getMetrics() {
        return new String[] { "calls", "time", "errors" };
    }


    @Override
    public String[] getAverages() {
        return new String[]  { "CUR", "AVG1", "AVG5", "AVG15" };
    }


    @Override
    public MethodCallStatistic getWrapped() {
        return this;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public String getDescription() {
        return "Number of calls (as measured by Zorka Spy) and its summary time.";
    }


    @Override
    public String getUnit() {
        return "MILLISECOND";
    }


    /**
     * Returns aggregate index based on (approximate) time window caller is interested in
     *
     * @param window time window (in base units)
     *
     * @return integer that can be used to retrieve specific aggregate for all metrics
     */
    public int getStage(long window) {
        return calls.getStage(window);
    }


    /**
     * Returns total number of calls to a method
     *
     * @return number of calls
     */
	public synchronized long getCalls() {
		return calls.getTotal();
	}


    /**
     * Returns number of errors (exceptions thrown out of a method)
     *
     * @return number of errors
     */
	public synchronized long getErrors() {
		return errors.getTotal();
	}


    /**
     * Returns total execution time (sum of execution times of all calls to a method)
     *
     * @return total execution time
     */
	public synchronized long getTime() {
		return time.getTotal()/MS;
	}


    /**
     * Returns timestamp of last call to a method
     *
     * @return timestamp
     */
    public synchronized Date lastSample() {
        return new Date(calls.getLast()/MS);
    }


    /**
     * Logs successful method call
     *
     * @param tstamp method call timestamp
     *
     * @param time execution time
     */
	public synchronized void logCall(long tstamp, long time) {
        this.calls.feed(tstamp, 1);
        this.time.feed(tstamp, time);
	}


    /**
     * Logs unsucessful method call (with error thrown out of method).
     * Note that you don't need to
     *
     * @param tstamp method call timestamp
     *
     * @param time execution time
     */
	public synchronized void logError(long tstamp, long time) {
        this.calls.feed(tstamp, 1);
        this.errors.feed(tstamp, 1);
        this.time.feed(tstamp, time);
    }


    @Override
    public String toString() {
        long tstamp = System.nanoTime();
        return "(calls=" + getCalls()
                + ", errors=" + getErrors()
                + ", time=" + getTime()
                +", avg1=" + getAverage(tstamp, TIMES_STAT, 0)
                + ", avg5=" + getAverage(tstamp, TIMES_STAT,  1)
                + ", avg15=" + getAverage(tstamp, TIMES_STAT, 2) + ")";
    }
}
