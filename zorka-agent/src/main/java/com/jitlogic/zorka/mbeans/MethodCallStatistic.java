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

import static com.jitlogic.zorka.rankproc.BucketAggregate.MS;
import com.jitlogic.zorka.rankproc.BucketAggregate;
import com.jitlogic.zorka.rankproc.Rankable;

import java.util.Date;

public class MethodCallStatistic implements ZorkaStat, Rankable<MethodCallStatistic> {

    public final static int CALLS_STAT = 0;
    public final static int TIMES_STAT = 1;
    public final static int ERROR_STAT = 2;

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


    public long getTotal(int metric) {
        switch (metric) {
            case CALLS_STAT:
                return calls.getTotal();
            case ERROR_STAT:
                return errors.getTotal();
            case TIMES_STAT:
                return time.getTotal() / MS;
        }

        return -1;
    }


    public double getAverage(int metric, int average) {
        switch (metric) {
            case CALLS_STAT: {
                long delta = calls.getDelta(average);
                return 1000.0 * delta / (calls.getWindow(average) * MS);
            }
            case ERROR_STAT: {
                long delta = errors.getDelta(average);
                return 1000.0 * delta / (errors.getWindow(average) * MS);
            }
            case TIMES_STAT: {
                long dc = calls.getDelta(average), dt = time.getDelta(average);
                return dc == 0 ? 0.0 : 1000.0 * dt / (dc * MS);
            }
        }
        return 0.0;
    }


    public String[] getMetrics() {
        return new String[] { "calls", "time", "errors" };
    }


    public String[] getAverages() {
        return new String[]  { "CUR", "AVG1", "AVG5", "AVG15" };
    }


    public MethodCallStatistic getWrapped() {
        return this;
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



	public synchronized long getErrors() {
		return errors.getTotal();
	}


	public synchronized long getTime() {
		return time.getTotal()/MS;
	}


    public synchronized Date lastSample() {
        return new Date(calls.getLast()/MS);
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
