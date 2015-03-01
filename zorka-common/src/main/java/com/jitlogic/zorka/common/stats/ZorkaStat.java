/**
 *
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.jitlogic.zorka.common.stats;

/**
 * Base interface for all statistics reported by Zorka.
 *
 */
public interface ZorkaStat {

    /**
     * @return The name of this Statistic.
     */
    String getName();

    /**
     * @return The unit of measurement for this Statistic.
     *         Valid values for TimeStatistic measurements
     *         are "HOUR", "MINUTE", "SECOND", "MILLISECOND",
     *         "MICROSECOND" and "NANOSECOND".
     */
    String getUnit();

    /**
     * @return A human-readable description of the Statistic.
     */
    String getDescription();
}
