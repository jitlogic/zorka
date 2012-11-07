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


import java.util.Date;

public class MethodCallStatistic implements MethodCallStat {

	public static final long NS = 1000000;
	
	private String name;
	
	private long totalTimeNs = 0, tstampNs = 0;
	
	private long calls = 0, errors = 0;

    public MethodCallStatistic(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return "Number of calls (as measured by Zorka Spy) and its summary time.";
    }

    public String getUnit() {
        return "MILLISECOND";
    }

	public synchronized long getCalls() {
		return calls;
	}
	
	public synchronized long getErrors() {
		return errors;
	}
	
	public synchronized long getTime() {
		return totalTimeNs/NS;
	}

	public synchronized void logCall(long tst, long ns) {
		calls++; totalTimeNs += ns;
        tstampNs = tst;
	}
	
	public synchronized void logError(long tst, long ns) {
		calls++; errors++; totalTimeNs += ns;
        tstampNs = tst;
    }

    public synchronized Date getLastCallTime() {
        return new Date(tstampNs/NS);
    }
}
