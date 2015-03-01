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

package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * Calculates time difference between time stamps
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TimeDiffProcessor implements SpyProcessor {

    /**
     * Field containing start timestamp
     */
    private String tstart;

    /**
     * Field containing stop timestamp
     */
    private String tstop;

    /**
     * Field containing result timestamp
     */
    private String rslt;

    /**
     * Creates time difference calculating processor
     *
     * @param tstart start timestamp field
     * @param tstop  stop timestamp field
     * @param rslt   result field
     */
    public TimeDiffProcessor(String tstart, String tstop, String rslt) {
        this.tstart = tstart;
        this.tstop = tstop;
        this.rslt = rslt;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object v1 = record.get(tstart),
                v2 = record.get(tstop);

        if (v1 instanceof Long && v2 instanceof Long) {
            long l1 = (Long) v1, l2 = (Long) v2;
            record.put(rslt, l2 - l1);
        } // TODO else (log something here ?)

        return record;
    }

}
