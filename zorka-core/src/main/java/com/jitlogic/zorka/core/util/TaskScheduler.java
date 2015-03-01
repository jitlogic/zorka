/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Trivial singleton wrapper around ScheduledExcutorService.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TaskScheduler {

    /** Scheduler instance. */
    protected static TaskScheduler instance = new TaskScheduler();

    /** Returns task scheduler instance. */
    public static TaskScheduler instance() {
        return instance;
    }

    /** Executor */
    private ScheduledExecutorService executor;

    /** Hidden constructor. Use instance() method to get scheduler instance. */
    protected TaskScheduler() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Schedules task to be run at fixed intervals.
     *
     * @param runnable runnable task
     *
     * @param interval interval
     * @param delay
     */
    public void schedule(Runnable runnable, long interval, long delay) {
        executor.scheduleAtFixedRate(runnable, delay, interval, TimeUnit.MILLISECONDS);
    }
}
