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

import java.util.HashMap;
import java.util.Map;

import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;

public class MethodCallStats implements Stats {

	private Map<String, MethodCallStatisticImpl> stats = new HashMap<String, MethodCallStatisticImpl>();

	
	public synchronized Statistic getStatistic(String statisticName) {
		return stats.get(statisticName);
	}
	
	
	public synchronized String[] getStatisticNames() {
		String[] names = new String[stats.size()];
		
		int i = 0; 

		for (String name : stats.keySet()) {
			names[i++] = name;
		}
		
		return names;
	}
	
	
	public synchronized Statistic[] getStatistics() {
		Statistic[] st = new Statistic[stats.size()];
		
		int i = 0; 
		
		for (Statistic stat : stats.values()) {
			st[i++] = stat;
		}
		
		return st;
	}
	
	
	public synchronized MethodCallStatisticImpl getMethodCallStat(String name) {
		MethodCallStatisticImpl ret = stats.get(name);
		
		if (ret == null) {
			ret = new MethodCallStatisticImpl();
			stats.put(name,  ret);
		}
		
		return ret;
	}

	public synchronized void clear() {
		stats.clear();
	}
}
