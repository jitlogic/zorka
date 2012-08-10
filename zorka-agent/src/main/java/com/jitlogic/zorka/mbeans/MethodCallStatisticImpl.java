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
