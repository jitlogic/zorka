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

package com.jitlogic.zorka.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 *
 *     TODO wyczyscic ten mess i zrobic to porzadnie
 */
public class ClosingTimeoutExecutor implements Executor {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private List<Thread>     runnerThreads;
    private List<RunnerTask> runnerTasks;
    private Thread reaperThread;

    private BlockingQueue<Runnable> queuedTasks;

    private long timeout;

    public volatile long numRuns = 0, numInterruptions = 0, numTimeouts = 0, numErrors = 0, numDrops = 0;
    public volatile long numReapInterruptions = 0, numReapKills = 0;


    public ClosingTimeoutExecutor(int nthreads, int qlength, long timeout) {
        //queuedTasks = new ArrayBlockingQueue<Runnable>(qlength);
        queuedTasks = new SynchronousQueue<Runnable>();
        runnerThreads = new ArrayList<Thread>(nthreads+2);
        runnerTasks = new ArrayList<RunnerTask>(nthreads+2);
        this.timeout = timeout;

        for (int i = 0; i < nthreads; i++) {
            RunnerTask task = new RunnerTask();
            Thread thread = new Thread(task);
            runnerTasks.add(task);
            runnerThreads.add(thread);
            thread.setName("ZORKA-runner-" + i);
            thread.start();
        }

        reaperThread = new Thread(new ReaperTask());
        reaperThread.start();
    }


    public void execute(Runnable task) {
        for (int i = 0; i < 100; i++) {
            try {
                if (!queuedTasks.offer(task, 1, TimeUnit.MILLISECONDS)) {
                    log.info("Task queue overflow. Retrying");
                    //closeTask(task);
                    numDrops++;
                } else {
                    return;
                }
            } catch (InterruptedException e) {
                log.info("Interrupted while enqueueing. Retrying");
                //closeTask(task);
                numDrops++;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {

            }
        }
        log.info("Task not enqueued properly, killing ...");
        closeTask(task);
    }


    private class RunnerTask implements Runnable {

        private volatile boolean running = true;
        private long timestamp = 0;
        //private volatile Runnable task = null;

        public void run() {
            Thread.currentThread().setName("ZORKA-reaper-thread");
            while (running) {
                Runnable task = null;
                try {
                    task = queuedTasks.poll(10, TimeUnit.MILLISECONDS);
                    if (task == null) continue;
                    timestamp = System.currentTimeMillis();
                    //System.out.println("Starting task: " + task);
                    task.run();
                    numRuns++;
                } catch (InterruptedException e) {
                    numInterruptions++;
                    log.info("Closing task because of InterruptedException.");
                    //System.out.println("Got interruptException ...");
                    // nothing interesting here, just try again
                    closeTask(task);
                } catch (TimeoutException e) {
                    numTimeouts++;
                    log.info("Closing task because of TimeoutException.");
                    closeTask(task);
                } catch (Exception e) {
                    log.error("Exception during task execution: ", e);
                    //System.out.println("Error: " + e);
                    //e.printStackTrace();
                    numErrors++;
                    closeTask(task); // TODO error handling here
                }
                timestamp = 0;
                //task = null;
            }
        }


        public void stop() {
            running = false;
        }


        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

//        public Runnable getTask() {
//            return task;
//        }
    }


    private static class TimeoutException extends Error {

    }


    private class ReaperTask implements Runnable {

        public volatile boolean running = true;

        public void run() {
            while (running) {
                    try {
                        Thread.sleep(5);
                    long time = System.currentTimeMillis();
                    for (int i = 0; i < runnerTasks.size(); i++) {

                        //long td = time - runnerTasks.get(i).getTimestamp();
                        //System.out.println("" + time + ": tdif = " + td + ", timeout = " + timeout);
                        Thread thread = runnerThreads.get(i);
                        RunnerTask task = runnerTasks.get(i);

                        long ts = task.getTimestamp();
                        long tdif = time - ts;

                        if (ts > 0 && tdif > 86400000) {
                            log.info("Task time fuzz too big, resetting tdif ...");
                            task.setTimestamp(time);
                        }

                        if (ts > 0 && tdif > timeout) {
                            log.info("Killing thread ..." + i + "(tst=" + ts + ", time=" + time + ")");
                            thread.interrupt();
                            thread.join(1);
                            if (thread.isAlive()) {
                                //System.out.println("Killing task: " + task.getTask());
                                thread.stop(new TimeoutException());
                                numReapKills++;
                                //closeTask(runnerTasks.get(i).getTask());
                            } else {
                                numReapInterruptions++;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

        public void stop() {
            running = false;
        }

    }


    public void closeTask(Runnable task) {
        if (task != null && task instanceof Closeable) {
            try {
                ((Closeable)task).close();
            } catch (IOException e) {
                // TODO log something here ?
                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }


    public int size() {
        int sz = 0;

//        for (RunnerTask t : runnerTasks)
//            if (t.getTask() != null) sz++;

        return sz + queuedTasks.size();
    }

}
