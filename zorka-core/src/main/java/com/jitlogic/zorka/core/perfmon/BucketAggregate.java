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


/**
 * Represents averages aggregators. Typically aggregators are used with nanosecond resolution,
 * so there are some constants representing time durations in nanoseconds.
 *
 * @author rafal.lewczuk@gmail.com
 */
public interface BucketAggregate {

    /** One nanosecond */
    long NS   = 1L;

    long US   = NS * 1000;

    /** One miliseccond (in nanoseconds) */
    long MS   = US * 1000;

    /** One second (in nanoseconds) */
    long SEC  = MS * 1000;

    /** One minute (in nanoseconds)*/
    long MIN  = SEC * 60;

    /** One hour (in nanseconds) */
    long HOUR = MIN * 60;

    /** One day (in nanoseconds) */
    long DAY  = HOUR * 24;


    /**
     * Returns number of time windows (stages) this aggregate tracks.
     *
     * @return number of calculated aggregates
     */
    int size();


    /**
     * Returns window length on particular aggregate.
     *
     * @param idx aggregate index
     *
     * @return window length
     */
    long getWindow(int idx);


    /**
     * Returns stage number (based on window length).
     *
     * @param window window length
     *
     * @return aggregate index
     */
    int getStage(long window);


    /**
     * Returns total time this aggregate is up and logging values (that is, time
     * elapsed between first and last value.
     *
     * @return aggregate uptime
     */
    long getTime();


    /**
     * Returns timestamp of last submitted value.
     *
     * @return timestamp of last submitted value
     */
    long getLast();


    /**
     * Return sum of all submitted values.
     *
     * @return sum of all submitted values
     */
    long getTotal();


    /**
     * Returns timestamp of first submitted value.
     *
     * @return timestamp of first submitted value
     */
    long getStart();


    /**
     * Submits (tstamp,value) pair to aggregate.
     *
     * @param tstamp timestamp
     *
     * @param value actual value
     */
    void feed(long tstamp, long value);


    /**
     * Returns value delta in given stage
     *
     *
     * @param stage stage
     *
     * @param tstamp timestamp
     *
     * @return value delta
     */
    long getDeltaV(int stage, long tstamp);


    /**
     * Returns time delta in given stage
     *
     *
     * @param stage stage
     *
     * @param tstamp timestamp
     *
     * @return time delta
     */
    long getDeltaT(int stage, long tstamp);
}

