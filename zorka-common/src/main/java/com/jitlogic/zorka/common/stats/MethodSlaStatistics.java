/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class MethodSlaStatistics implements ZorkaStats {

    private ConcurrentMap<Integer,MethodSlaStatistic> stats = new ConcurrentHashMap<Integer, MethodSlaStatistic>();


    @Override
    public MethodSlaStatistic getStatistic(String statisticName) {
        int threshold = Integer.parseInt(statisticName);
        MethodSlaStatistic rslt = stats.get(threshold);

        if (rslt == null) {
            MethodSlaStatistic st = stats.putIfAbsent(threshold, rslt = new MethodSlaStatistic(threshold));
            if (st != null) {
                rslt = st;
            }
        }

        return rslt;
    }


    @Override
    public String[] getStatisticNames() {
        List<String> lst = new ArrayList<String>(stats.size());
        for (Integer i : stats.keySet()) {
            lst.add(i.toString());
        }
        return lst.toArray(new String[lst.size()]);
    }


    public void logCall(long t) {
        for (Map.Entry<Integer,MethodSlaStatistic> e : stats.entrySet()) {
            e.getValue().logCall(t);
        }
    }


    public void logError(long t) {
        for (Map.Entry<Integer,MethodSlaStatistic> e : stats.entrySet()) {
            e.getValue().logError(t);
        }
    }


    @Override
    public String toString() {
        return "MethodSlaStatistics(" + stats + ")";
    }
}
