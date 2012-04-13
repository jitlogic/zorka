package com.jitlogic.zorka.spy;

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
		logStart(id, null);
	}
	
	
	public static void logStart(long id, Object[] args) {
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
