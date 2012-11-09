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


import com.jitlogic.zorka.rankproc.BucketAggregate;

import java.util.Date;

public class MethodCallStatistic implements ZorkaStat {

	private String name;

    private BucketAggregate calls;
    private BucketAggregate errors;
    private BucketAggregate time;


    public static MethodCallStatistic newStatAvg15(String name) {
        return new MethodCallStatistic(name, 10*BucketAggregate.SEC, 6, 5, 3);
    }


    public MethodCallStatistic(String name, long base, int...stages) {
        this.name = name;
        this.calls = new BucketAggregate(base, stages);
        this.errors = new BucketAggregate(base, stages);
        this.time = new BucketAggregate(base, stages);
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


    public int getStage(long window) {
        return calls.getStage(window);
    }


	public synchronized long getCalls() {
		return calls.getTotal();
	}


    public double getCallsAvg(long window) {
        return getCallsAvg(getStage(window));
    }


    public synchronized double getCallsAvg(int stage) {
        long cdelta = calls.getDelta(stage), tdelta = calls.getDelta(stage)/BucketAggregate.MS;
        return tdelta > 0 ? 1000.0 * cdelta / tdelta : 0.0;
    }


	public synchronized long getErrors() {
		return errors.getTotal();
	}


    public double getErrorsAvg(long window) {
        return getErrorsAvg(getStage(window));
    }


    public synchronized double getErrorsAvg(int stage) {
        long edelta = errors.getDelta(stage), tdelta = calls.getDelta(stage)/BucketAggregate.MS;
        return edelta > 0 ? 1000.0 * edelta / tdelta : 0.0;
    }


	public synchronized long getTime() {
		return time.getTotal()/BucketAggregate.MS;
	}


    public synchronized Date lastTime() {
        return new Date(calls.getLast()/BucketAggregate.MS);
    }


	public synchronized void logCall(long tstamp, long time) {
        this.calls.feed(tstamp, 1);
        this.time.feed(tstamp, time);
	}


	public synchronized void logError(long tstamp, long time) {
        this.calls.feed(tstamp, 1);
        this.errors.feed(tstamp, 1);
        this.time.feed(tstamp, time);
    }
}
