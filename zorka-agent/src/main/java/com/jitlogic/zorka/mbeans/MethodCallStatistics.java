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

import com.jitlogic.zorka.rankproc.RankLister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups statistics for multiple monitored methods.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MethodCallStatistics implements ZorkaStats, RankLister<MethodCallStatistic> {

    /** Map of method call statistics objects. */
	private HashMap<String, MethodCallStatistic> stats = new HashMap<String, MethodCallStatistic>();

	@Override
	public synchronized ZorkaStat getStatistic(String statisticName) {
		return stats.get(statisticName);
	}
	
	@Override
	public synchronized String[] getStatisticNames() {
		String[] names = new String[stats.size()];
		
		int i = 0; 

		for (String name : stats.keySet()) {
			names[i++] = name;
		}
		
		return names;
	}

    /**
     * Returns named statistic. If there is no such statistic, a new one is created and registered.
     *
     * @param name statistic (method) name
     *
     * @return method call statistic
     */
	public synchronized MethodCallStatistic getMethodCallStatistic(String name) {
		MethodCallStatistic ret = stats.get(name);
		
		if (ret == null) {
			ret = MethodCallStatistic.newStatAvg15(name);  // TODO make it configurable somewhere ...
			stats.put(name,  ret);
		}
		
		return ret;
	}


    @Override
    public synchronized List<MethodCallStatistic> list() {
        ArrayList<MethodCallStatistic> lst = new ArrayList<MethodCallStatistic>(stats.size()+2);

        for (Map.Entry<String, MethodCallStatistic> entry : stats.entrySet()) {
            lst.add(entry.getValue());
        }

        return lst;
    }

    @Override
    public synchronized String toString() {
        return stats.toString();
    }
}
