/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.mbeans;

/**
 * Implementations of this interface are capable of maintaining multiple
 * zorka statistics identified by name. This is equivalent of J2EE Statistics.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public interface ZorkaStats {

    /**
     * Returns named statistic (or null if no such statistic has been found)
     *
     * @param statisticName statistic name
     *
     * @return zorka statistic object or null
     */
    ZorkaStat getStatistic(String statisticName);

    /**
     * Returns names of all statistics maintained by this object.
     *
     * @return array of all statistic names
     */
    String[] getStatisticNames();
}
