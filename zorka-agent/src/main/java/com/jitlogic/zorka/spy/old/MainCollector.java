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

package com.jitlogic.zorka.spy.old;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class MainCollector {
	
	private static ThreadLocal<Stack<CallInfo>> callStacks;
	private static Map<Long,DataCollector> collectors;
	private static long lastId;
	
	
	public static synchronized long register(DataCollector collector) {
		long id = lastId++;
		collectors.put(id, collector);
		return id;
	}
	
	
	public static void logStart(long id) {
		logStart(null, id);
	}
	
	
	public static void logStart(Object[] args, long id) {
		DataCollector collector = collectors.get(id);
		if (collector != null) {
			CallInfo ci = collector.logStart(id, System.nanoTime(), args);
			callStacks.get().add(ci);
		} // TODO else { <log error here> }
	}
	
	
	private static CallInfo lookupInfo(long id) {
		return callStacks.get().pop(); // TODO chack if ID is right
	}
	
	
	public static void logCall(long id) {
		CallInfo ci = lookupInfo(id);
		if (ci != null) {
			long tst = System.nanoTime();
			DataCollector collector = collectors.get(id);
			if (collector != null) {
				collector.logCall(tst, ci);
			}
		}
	}
	
	
	public static void logError(long id) {
		CallInfo ci = lookupInfo(id);
		if (ci != null) {
			long tst = System.nanoTime();
			DataCollector collector = collectors.get(id);
			if (collector != null) {
				collector.logError(tst, ci);
			}
		}		
	}
	
	
	public static void clear() {
		callStacks = new ThreadLocal<Stack<CallInfo>>() {
			@Override
			public Stack<CallInfo> initialValue() {
				return new Stack<CallInfo>();
			}
		};
		collectors = new ConcurrentHashMap<Long, DataCollector>();
		lastId = 0L;
	}
	
	static {
		clear();
	}
}
