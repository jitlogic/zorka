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
package com.jitlogic.zorka.common.util;

import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.ZorkaSubmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements asunchronous processing thread with submit queue.
 *
 * @param <T> type of elements in a queue
 *            <p/>
 *            TODO factor out direct processing functionality (exposing process(), flush(), open(), close() etc.)
 */
public abstract class ZorkaAsyncThread<T> implements Runnable, ZorkaService, ZorkaSubmitter<T> {

    /**
     * Logger
     */
    protected final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Submit queue
     */
    protected BlockingQueue<T> submitQueue;

    /**
     * Thred name (will be prefixed with ZORKA-)
     */
    private final String name;

    /**
     * Processing thread will be working as long as this attribute value is true
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Thread object representing actual processing thread.
     */
    private Thread thread;

    protected boolean countTraps = true;

    private int plen;
    
    /**
     * Sleeping interval in milliseconds
     */
    private long interval = 0l ;

    public ZorkaAsyncThread(String name) {
        this(name, 256, 1);
    }

    /**
     * Standard constructor.
     *
     * @param name thread name
     * @param plen
     */
    public ZorkaAsyncThread(String name, int qlen, int plen) {
        this.name = "ZORKA-" + name;
        this.plen = plen;
        submitQueue = new ArrayBlockingQueue<T>(qlen);
    }
    
    /**
     * Constructor with interval
     *
     * @param name thread name
     * @param plen
     * @param interval in seconds
     */
    public ZorkaAsyncThread(String name, int qlen, int plen, int interval) {
    	this(name, qlen, plen);
    	
    	// convert to millis
        this.interval = interval * 1000l;
    }
    

    /**
     * This method starts thread.
     */
    public void start() {
        synchronized (this) {
            if (thread == null) {
                try {
                    open();
                    thread = new Thread(this);
                    thread.setName(name);
                    thread.setDaemon(true);
                    running.set(true);
                    thread.start();
                } catch (Exception e) {
                    handleError("Error starting thread", e);
                }
            }
        }
    }

    /**
     * This method causes thread to stop (soon).
     */
    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        while (running.get()) {
            runCycle();
            
            try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }

        synchronized (this) {
            close();
            thread = null;
        }
    }

    /**
     * Processes single item from submit queue (if any).
     */
    public void runCycle() {
        try {
            T obj = submitQueue.take();
            if (obj != null) {
                List<T> lst = new ArrayList<T>(plen);
                lst.add(obj);
                if (plen > 1) {
                    submitQueue.drainTo(lst, plen-1);
                }
                process(lst);
                flush();
            }
        } catch (InterruptedException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot perform run cycle", e);
        }
    }

    /**
     * Submits object to a queue.
     *
     * @param obj object to be submitted
     */
    public boolean submit(T obj) {
        try {
            return submitQueue.offer(obj, 1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }


    protected abstract void process(List<T> obj);


    /**
     * Override this method if some resources have to be allocated
     * before thread starts (eg. network socket).
     */
    public void open() {

    }


    /**
     * Override this method if some resources have to be disposed
     * after thread stops (eg. network socket)
     */
    public void close() {

    }


    /**
     * Flushes unwritten data to disk if necessary.
     */
    protected void flush() {

    }

    public void shutdown() {
        close();
        stop();
    }

    /**
     * Error handling method - called when processing errors occur.
     *
     * @param message error message
     * @param e       exception object
     */
    protected void handleError(String message, Throwable e) {
        if (log != null) {
            log.error(ZorkaLogger.ZAG_ERRORS, message, e);
        }
    }

    public void disableTrapCounter() {
        countTraps = false;
    }

    public BlockingQueue<T> getSubmitQueue() {
        return submitQueue;
    }
}
