/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements asunchronous processing thread with submit queue.
 *
 * @param <T> type of elements in a queue
 */
public abstract class ZorkaAsyncThread<T> implements Runnable {

    /** Logger */
    protected final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Submit queue */
    private final LinkedBlockingQueue<T> submitQueue = new LinkedBlockingQueue<T>(1024);

    /** Thred name (will be prefixed with ZORKA-) */
    private final String name;

    /** Processing thread will be working as long as this attribute value is true */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Thread object representing actual processing thread. */
    private Thread thread;

    private boolean flushNeeded;

    /**
     * Standard constructor.
     *
     * @param name thread name
     */
    public ZorkaAsyncThread(String name) {
        this.name = "ZORKA-"+name;
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
        }

        synchronized (this) {
            close();
            thread = null;
        }
    }

    /**
     * Processes single item from submit queue (if any).
     */
    private void runCycle() {
        try {
            T obj = submitQueue.poll(10, TimeUnit.MILLISECONDS);
            if (obj != null) {
                process(obj);
                flushNeeded = true;
            } else {
                if (flushNeeded) {
                    flush();
                    flushNeeded = false;
                }
            }
        } catch (InterruptedException e) {
            log.error("Cannot perform run cycle", e);
        }
    }

    /**
     * Submits object to a queue.
     *
     * @param obj object to be submitted
     */
    public void submit(T obj) {
        try {
            submitQueue.offer(obj, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    protected abstract void process(T obj);


    /**
     * Override this method if some resources have to be allocated
     * before thread starts (eg. network socket).
     */
    protected void open() {

    }


    /**
     * Override this method if some resources have to be disposed
     * after thread stops (eg. network socket)
     */
    protected void close() {

    }


    /**
     * Flushes unwritten data to disk if necessary.
     */
    protected void flush() {

    }

    /**
     * Error handling method - called when processing errors occur.
     *
     * @param message error message
     *
     * @param e exception object
     */
    protected void handleError(String message, Throwable e) {
        if (log != null) {
            log.error(message, e);
        }
    }
}
