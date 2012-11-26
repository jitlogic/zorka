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


/**
 * Represents aggregate values.
 * TODO clean up words and symbols (eg. stage-window)
 */
public interface BucketAggregate {

    long NS   = 1L;
    long MS   = NS * 1000000;
    long SEC  = MS * 1000;
    long MIN  = SEC * 60;
    long HOUR = MIN * 60;
    long DAY  = HOUR * 24;


    /**
     * Returns number of time windows (stages) this aggregate tracks.
     *
     * @return
     */
    int size();


    /**
     * Returns window length on particular stage.
     *
     * @param stage
     *
     * @return
     */
    long getWindow(int stage);


    /**
     * Returns stage number (based on window length).
     *
     * @param window
     *
     * @return
     */
    int getStage(long window);


    /**
     * Returns total time this aggregate is up and logging samples (that is, time
     * elapsed between
     *
     * @return
     */
    long getTime();


    /**
     * Returns timestamp of last sample.
     *
     * @return
     */
    long getLast();


    /**
     * Return sum of all submitted samples.
     *
     * @return
     */
    long getTotal();


    /**
     * Returns timestamp of first submitted sample.
     *
     * @return
     */
    long getStart();


    /**
     * Submits (tstamp,value) pair to aggregate.
     *
     * @param tstamp
     *
     * @param value
     */
    void feed(long tstamp, long value);


    /**
     * Returns value delta in given stage
     * @param tstamp
     * @param stage
     * @return
     */
    long getDeltaV(long tstamp, int stage);


    long getDeltaT(long tstamp, int stage);
}

