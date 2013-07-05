/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents statistics of calls to a single method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MethodCallStatistic implements ZorkaStat {

    private static final long US = 1000L;
    private static final long MS = 1000000L;

    /** Statistic name */
    private String name;

    /** Summary data. */
    private AtomicLong calls, errors, time, maxTime;

    private AtomicLong curThreads, maxThreads;

    /**
     * Standard constructor.
     *
     * @param name statistic name
     *
     *
     */
    public MethodCallStatistic(String name) {
        this.name = name;
        this.calls = new AtomicLong(0);
        this.errors = new AtomicLong(0);
        this.time = new AtomicLong(0);
        this.maxTime = new AtomicLong(0);

        this.curThreads = new AtomicLong(0);
        this.maxThreads = new AtomicLong(0);
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
     * @return total execution time (milliseconds)
     */
	public long getTime() {
		return time.longValue()/MS;
	}


    /**
     * Returns total execution time (sum of execution times of all calls to a method)
     *
     * @return total execution time (microseconds)
     */
    public long getTimeUs() {
        return time.longValue()/US;
    }


    /**
     * Returns total execution time (sum of execution times of all calls to a method)
     *
     * @return total execution time (nanoseconds)
     */
    public long getTimeNs() {
        return time.longValue();
    }


    /**
     * Returns maximum execution time (since last maxTimeCLR);
     *
     * @return maximum execution time (in milliseconds)
     */
    public long getMaxTime() {
        return maxTime.longValue()/MS;
    }


    /**
     * Returns maximum execution time (since last maxTimeCLR);
     *
     * @return maximum execution time (in microseconds)
     */
    public long getMaxTimeUs() {
        return maxTime.longValue()/US;
    }


    /**
     * Returns maximum execution time (since last maxTimeCLR);
     *
     * @return maximum execution time (in nanoseconds)
     */
    public long getMaxTimeNs() {
        return maxTime.longValue();
    }

    /**
     * Returns maximum execution time and zeroes maxTime field
     * in thread safe manner.
     *
     * @return maximum execution time (in milliseconds)
     */
    public long getMaxTimeCLR() {
        return getMaxTimeNsCLR()/MS;
    }


    /**
     * Returns maximum execution time and zeroes maxTime field
     * in thread safe manner.
     *
     * @return maximum execution time (in microseconds)
     */
    public long getMaxTimeUsCLR() {
        return getMaxTimeNsCLR()/US;
    }


    /**
     * Returns maximum execution time and zeroes maxTime field
     * in thread safe manner.
     *
     * @return maximum execution time (in milliseconds)
     */
    public long getMaxTimeNsCLR() {
        return getMaxCLR(maxTime);
    }


    private long getMaxCLR(AtomicLong counter) {
        long t = counter.longValue();

        while (!counter.compareAndSet(t, 0)) {
            t = counter.longValue();
        }

        return t;
    }


    public void markEnter() {
        setMax(maxThreads, curThreads.incrementAndGet());
    }


    public void markExit() {
        curThreads.decrementAndGet();
    }


    public long getMaxThreads() {
        return maxThreads.get();
    }


    public long getMaxThreadsCLR() {
        return getMaxCLR(maxThreads);
    }

    public long getCurThreads() {
        return curThreads.get();
    }


    /**
     * Sets maximum value to an atomic counter
     * in thread safe manner.
     *
     * @param v sample value
     *
     */
    private void setMax(AtomicLong counter, long v) {
        long v2 = counter.longValue();

        while (v > v2) {
            if (!counter.compareAndSet(v2, v)) {
                v2 = counter.longValue();
            } else {
                return;
            }
        }
    }


    /**
     * Logs successful method call
     *
     * @param time execution time
     */
	public void logCall(long time) {
        this.calls.incrementAndGet();
        this.time.addAndGet(time);
        this.setMax(maxTime, time);
	}


    /**
     * Logs unsucessful method call (with error thrown out of method).
     * Note that you don't need to
     *
     * @param time execution time
     */
	public void logError(long time) {
        this.errors.incrementAndGet();
        this.calls.incrementAndGet();
        this.time.addAndGet(time);
        this.setMax(maxTime, time);
    }


    @Override
    public String toString() {
        return "(calls=" + getCalls()
                + ", errors=" + getErrors()
                + ", time=" + getTime() + ")";
    }
}
