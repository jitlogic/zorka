/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
 * Stripped down version of standard ThreadInfo.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ThreadRankInfo {

    /** Thread ID */
    private final long id;

    /** Thread name */
    private final String name;

    /** CPU time */
    private final long cpuTime;

    /** Blocked time */
    private final long blockedTime;

    /**
     * Creates new ThreadRankInfo object.
     *
     * @param id thread ID
     *
     * @param name thread name
     *
     * @param cpuTime thread CPU time
     *
     * @param blockedTime thread blocked time
     */
    public ThreadRankInfo(long id, String name, long cpuTime, long blockedTime) {
        this.id = id;
        this.name = name;
        this.cpuTime = cpuTime;
        this.blockedTime = blockedTime;
    }

    /** Returns thread ID */
    public long getId() {
        return id;
    }

    /** Returns thread name */
    public String getName() {
        return name;
    }

    /** Returns thread CPU time */
    public long getCpuTime() {
        return cpuTime;
    }

    /** Returns thread blocked time */
    public long getBlockedTime() {
        return blockedTime;
    }
}
