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

package com.jitlogic.zorka.agent;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimeoutThreadPoolExecutor extends ThreadPoolExecutor {
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Runnable, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<Runnable, ScheduledFuture<?>>();
    
    
    public TimeoutThreadPoolExecutor(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, 
    		long timeout, TimeUnit timeoutUnit) {
    	
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new ZorkaWorkerThreadFactory());
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    
    @Override
    public void shutdown() {
        timeoutExecutor.shutdown();
        super.shutdown();
    }
    
    
    @Override
    public List<Runnable> shutdownNow() {
        timeoutExecutor.shutdownNow();
        return super.shutdownNow();
    }

    
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if(timeout > 0) {
            final ScheduledFuture<?> scheduled = timeoutExecutor.schedule(
            		new TimeoutTask(t, r), timeout, timeoutUnit);
            runningTasks.put(r, scheduled);
        }
    }
    
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            ScheduledFuture<?> timeoutTask = runningTasks.remove(r);
            if(timeoutTask != null) {
                timeoutTask.cancel(false);
            }
            if (t != null)
                closeTask(r);
        } finally {
            super.afterExecute(r, t);
        }
    }
    
    
    class TimeoutTask implements Runnable {
        private final Thread thread;
        private final Runnable task;


        public TimeoutTask(Thread thread, Runnable task) {
            this.thread = thread;
            this.task = task;
        }


        @SuppressWarnings("deprecation")
        public void run() {
            thread.interrupt();

            try {
            	Thread.sleep(10);
            	// If thread did not finish, kill it forcibly (not safe)
            	if (thread.isAlive()) {
            		thread.stop();
                }
            } catch (InterruptedException e) {
            	// TODO some error logging here ?
            } finally {
                closeTask(task);
            }
        }

    }


    public static class ZorkaWorkerThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {
            return new Thread(r, "ZORKA-WORKIER");
        }
    }


    public static void closeTask(Runnable task) {
        if (task instanceof Closeable)
            try {
                ((Closeable)task).close();
            } catch (Exception e) {
                // TODO log something here ?
            }
    }


    public static ExecutorService newBoundedPool(long timeout) {
    	ThreadPoolExecutor ex = new TimeoutThreadPoolExecutor(8, 8, 2000,
    		TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(32),
    		timeout, TimeUnit.MILLISECONDS);
        ex.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                closeTask(r);
            }
        });
        return ex;
    }
    
}
