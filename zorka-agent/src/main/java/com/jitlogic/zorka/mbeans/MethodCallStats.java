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

}
