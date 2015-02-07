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
 * Interface implemented by all wrappers used to present various types of data in rank lists.
 *
 * @param <T> wrapped type
 */
public interface Rankable<T> {

    /**
     * Returns given average of given metric at given time.
     *
     * @param tstamp current time
     *
     * @param metric metric index
     *
     * @param average average index
     *
     * @return average value
     */
    public double getAverage(long tstamp, int metric, int average);

    /**
     * Lists all metrics by name. Indexes of metrics in returned array can be used in all
     * methods requesting integer argument that identifies metric.
     *
     * @return metrics tracked by item
     */
    public String[] getMetrics();

    /**
     * Returns all averages tracked by item. Indexes of averages in returned array can be used in all
     * methods requesting integer argument that identifies metfic.
     *
     * @return averages tracked by item
     */
    public String[] getAverages();

    /**
     * Returns reference to wrapped object
     *
     * @return reference to wrapped object
     */
    public T getWrapped();

    /**
     * Returns visible item name.
     *
     * @return item name
     */
    public String getName();

}
