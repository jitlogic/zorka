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
	private final long killTimeout;
    private final TimeUnit timeoutUnit;

    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<Runnable, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<Runnable, ScheduledFuture<?>>();
    
    
    public TimeoutThreadPoolExecutor(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, 
    		long timeout, TimeUnit timeoutUnit, long killTimeout) {
    	
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.killTimeout = killTimeout;
    }
    
    
    public TimeoutThreadPoolExecutor(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, 
    		ThreadFactory threadFactory, long timeout, TimeUnit timeoutUnit,
    		long killTimeout) {
    	
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.killTimeout = killTimeout;
    }
    
    
    public TimeoutThreadPoolExecutor(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, 
    		RejectedExecutionHandler handler, long timeout, TimeUnit timeoutUnit,
    		long killTimeout) {
    	
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.killTimeout = killTimeout;
    }
    
    
    public TimeoutThreadPoolExecutor(int corePoolSize, int maximumPoolSize, 
    		long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, 
    		ThreadFactory threadFactory, RejectedExecutionHandler handler, 
    		long timeout, TimeUnit timeoutUnit, long killTimeout) {
    	
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        this.killTimeout = killTimeout;
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
    	//System.err.println("beforeExecute()");
        if(timeout > 0) {
            final ScheduledFuture<?> scheduled = timeoutExecutor.schedule(
            		new TimeoutTask(t), timeout, timeoutUnit);
            runningTasks.put(r, scheduled);
        }
    }
    
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
    	//System.err.println("AfterExecute()");
        ScheduledFuture<?> timeoutTask = runningTasks.remove(r);
        if(timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }
    
    
    class TimeoutTask implements Runnable {
        private final Thread thread;

        public TimeoutTask(Thread thread) {
            this.thread = thread;
        }

        @SuppressWarnings("deprecation")
        public void run() {
            thread.interrupt();
            if (killTimeout <= 0) return;
            try {
            	Thread.sleep(killTimeout);
            	// If thread did not finish, kill it forcibly (not safe)
            	if (thread.isAlive()) 
            		thread.stop();
            } catch (InterruptedException e) {
            	
            }
        }
    }

    
    public static ExecutorService newBoundedPool(int maxThreads, long timeout, long killTimeout) {
    	return new TimeoutThreadPoolExecutor(1, maxThreads, 2000, 
    		TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), 
    		timeout, TimeUnit.MILLISECONDS, killTimeout);
    }
    
}
