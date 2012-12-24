/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.agent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class ZorkaAsyncThread<T> implements Runnable {

    private LinkedBlockingQueue<T> submitQueue = new LinkedBlockingQueue<T>(1024);

    private String name;
    private volatile boolean running;
    private volatile Thread thread = null;


    public ZorkaAsyncThread(String name) {
        this.name = "ZORKA-"+name;
    }


    public synchronized void start() {
        if (thread == null) {
            try {
                open();
                thread = new Thread(this);
                thread.setName(name);
                thread.setDaemon(true);
                running = true;
                thread.start();
            } catch (Exception e) {
                handleError("Error starting thread", e);
            }
        }
    }


    public synchronized void stop() {
        running = false;
    }


    public void run() {
        while (running) {
            runCycle();
        }

        close();
        thread = null;
    }

    public synchronized void runCycle() {
        try {
            T obj = submitQueue.poll(10, TimeUnit.MILLISECONDS);
            if (obj != null) {
                process(obj);
            }
        } catch (InterruptedException e) {
        }
    }

    public void submit(T obj) {
        try {
            submitQueue.offer(obj, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    protected abstract void process(T obj);


    protected void open() {

    }


    protected void close() {

    }

    protected void handleError(String message, Throwable obj) {

    }
}
