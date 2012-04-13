package com.jitlogic.zorka.mbeans;


public class MethodCallStatisticImpl implements MethodCallStatistic {

	public static final long NS = 1000000;
	
	private String name;
	
	private long startTimeNs = Long.MIN_VALUE, lastSampleTimeNs = Long.MIN_VALUE;
	private long minTimeNs = Long.MAX_VALUE, maxTimeNs = Long.MIN_VALUE, totalTimeNs = 0;
	
	private long calls = 0, errors = 0;
	
	public String getDescription() {
		return "Number of calls (as measured by Zorka Spy) and its summary time.";
	}

	public synchronized long getLastSampleTime() {
		return lastSampleTimeNs/NS;
	}

	public String getName() {
		return name;
	}

	public synchronized long getStartTime() {
		return startTimeNs/NS;
	}

	public String getUnit() {
		return "ms";
	}

	public synchronized long getCount() {
		return calls+errors;
	}

	public synchronized long getCalls() {
		return calls;
	}
	
	public synchronized long getErrors() {
		return errors;
	}
	
	public synchronized long getMaxTime() {
		return maxTimeNs/NS;
	}

	public synchronized long getMinTime() {
		return minTimeNs/NS;
	}

	public synchronized long getTotalTime() {
		return totalTimeNs/NS;
	}

	public synchronized void logCall(long tst, long ns) {
		calls++; totalTimeNs += ns;
		
		if (ns < minTimeNs) { minTimeNs = ns; }
		if (ns > maxTimeNs) { maxTimeNs = ns; }
		if (startTimeNs == Long.MIN_VALUE) { startTimeNs = tst; }
		lastSampleTimeNs = tst;
	}
	
	public synchronized void logError(long tst, long ns) {
		calls++; errors++; totalTimeNs += ns;

		if (ns < minTimeNs) { minTimeNs = ns; }
		if (ns > maxTimeNs) { maxTimeNs = ns; }
		if (startTimeNs == Long.MIN_VALUE) { startTimeNs = tst; }
		lastSampleTimeNs = tst;
}
}
