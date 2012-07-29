package com.jitlogic.zorka.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ClosingTimeoutExecutor implements Executor {

    private List<Thread>     runnerThreads;
    private List<RunnerTask> runnerTasks;
    private Thread reaperThread;

    private BlockingQueue<Runnable> queuedTasks;

    private int queueLength;
    private long timeout;

    public volatile long numRuns = 0, numInterruptions = 0, numTimeouts = 0, numErrors = 0, numDrops = 0;
    public volatile long numReapInterruptions = 0, numReapKills = 0;


    public ClosingTimeoutExecutor(int nthreads, int qlength, long timeout) {
        //queuedTasks = new ArrayBlockingQueue<Runnable>(qlength);
        queuedTasks = new SynchronousQueue<Runnable>();
        runnerThreads = new ArrayList<Thread>(nthreads);
        runnerTasks = new ArrayList<RunnerTask>(nthreads);
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
        try {
            if (!queuedTasks.offer(task, 1, TimeUnit.MILLISECONDS)) {
                //System.out.println("discarding task: " + task);
                closeTask(task);
                numDrops++;
            } //else
                //System.out.println("accepted task: " + task);
        } catch (InterruptedException e) {
            //System.out.println("discarding task [2]: " + task);
            closeTask(task);
            numDrops++;
        }
    }


    private class RunnerTask implements Runnable {

        private volatile boolean running = true;
        private volatile long timestamp = 0;
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
                    //System.out.println("Got interruptException ...");
                    // nothing interesting here, just try again
                    closeTask(task);
                } catch (TimeoutException e) {
                    numTimeouts++;
                    closeTask(task);
                } catch (Exception e) {
                    System.out.println("Error: " + e);
                    e.printStackTrace();
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

                        if (task.getTimestamp() > 0 && time - task.getTimestamp() > timeout) {

                            //System.out.println("Interrupting thread ..." + i + "(tst=" + task.getTimestamp() + ")");
                            thread.interrupt();
                            thread.join(1);
                            if (thread.isAlive()) {
                                //System.out.println("Killing task: " + task.getTask());
                                thread.stop(new TimeoutException());
                                numReapKills++;
                                //closeTask(runnerTasks.get(i).getTask());
                            } else
                                numReapInterruptions++;
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
