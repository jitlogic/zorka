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
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.rankproc.BucketAggregate;
import com.jitlogic.zorka.rankproc.CircularBucketAggregate;
import com.jitlogic.zorka.rankproc.Rankable;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents statistics of calls to a single method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MethodCallStatistic implements ZorkaStat {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(MethodCallStatistic.class);

    /** Number of calls metric ID */
    public static final int CALLS_STAT = 0;

    /** Execution times metric ID */
    public static final int TIMES_STAT = 1;

    /** Number of errors metric ID */
    public static final int ERROR_STAT = 2;

    /** Statistic name */
    private String name;

    /** Summary data. */
    private AtomicLong calls, errors, time;

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
        this.calls = new AtomicLong(0);
        this.errors = new AtomicLong(0);
        this.time = new AtomicLong(0);
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
     * Returns total number of calls to a method
     *
     * @return number of calls
     */
	public long getCalls() {
		return calls.longValue();
	}


    /**
     * Returns number of errors (exceptions thrown out of a method)
     *
     * @return number of errors
     */
	public long getErrors() {
		return errors.longValue();
	}


    /**
     * Returns total execution time (sum of execution times of all calls to a method)
     *
     * @return total execution time
     */
	public long getTime() {
		return time.longValue()/MS;
	}



    /**
     * Logs successful method call
     *
     * @param tstamp method call timestamp
     *
     * @param time execution time
     */
	public void logCall(long tstamp, long time) {
        this.calls.incrementAndGet();
        this.time.addAndGet(time);
	}


    /**
     * Logs unsucessful method call (with error thrown out of method).
     * Note that you don't need to
     *
     * @param tstamp method call timestamp
     *
     * @param time execution time
     */
	public void logError(long tstamp, long time) {
        this.errors.incrementAndGet();
        this.calls.incrementAndGet();
        this.time.addAndGet(time);
    }


    @Override
    public String toString() {
        return "(calls=" + getCalls()
                + ", errors=" + getErrors()
                + ", time=" + getTime() + ")";
    }
}
