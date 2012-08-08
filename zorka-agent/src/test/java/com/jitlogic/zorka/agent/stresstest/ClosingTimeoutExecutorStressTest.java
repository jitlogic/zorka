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

package com.jitlogic.zorka.agent.stresstest;

import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.sun.jmx.snmp.tasks.Task;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ClosingTimeoutExecutorStressTest {

    public static class TestTask implements Runnable, Closeable {


        private volatile int runs = 0, closes = 0;
        private volatile long sleepTime;
        private int seq;

        public TestTask(int seq, long sleepTime) {
            this.seq = seq;
            this.sleepTime = sleepTime;
        }

        public void close() throws IOException {
            closes++;
        }

        public void run() {
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                    runs++; closes++;
                } catch (InterruptedException e) {
                    closes++;
                }
            } else {
                //System.out.println("pentla!");
                while (true) ;
            }
        }

        public int getRuns() {
            return runs;
        }

        public int getCloses() {
            return closes;
        }

        public long getSleepTime() {
            return sleepTime;
        }

        public int getSeq() {
            return seq;
        }

        public String toString() {
            return "T:" + getSeq() + " (t=" + getSleepTime() + ", r=" + getRuns() + ", c=" + getCloses() + ")";
        }


    }


    public void doStress(int n, int sl, int rnd) throws Exception {
        ClosingTimeoutExecutor executor = new ClosingTimeoutExecutor(8, 32, 80);
        List<TestTask> tasks = new ArrayList<TestTask>(n);
        Random r = new Random();

        System.out.println("Submitting tasks ...");
        for (int i = 0; i < n; i++) {
            int t = r.nextInt(rnd);
            TestTask task = new TestTask(i, t < 120 ? t : -1);
            Thread.sleep(sl);
            tasks.add(task);
            executor.execute(task);
        }

        //System.out.println("Waiting for threads to finish ...");
        Thread.sleep(300);

        int c0 = 0, c1 = 0, cn = 0;
        for (TestTask t : tasks) {
            switch (t.getCloses()) {
                case 0:
                    System.out.println("Broken task: " +t);
                    c0++;
                    break;
                case 1: c1++; break;
                case 2: cn++; break;
            }
        }

        System.out.println("Stats: c0=" + c0 + ", c1=" + c1 + ", cn=" + cn);
        System.out.println("numRuns=" + executor.numRuns);
        System.out.println("numInterruptions=" + executor.numInterruptions);
        System.out.println("numTimeouts=" + executor.numTimeouts);
        System.out.println("numErrors=" + executor.numErrors);
        System.out.println("numDrops=" + executor.numDrops);
        System.out.println("numReapInterruptions=" + executor.numReapInterruptions);
        System.out.println("numReapKills=" + executor.numReapKills);
    }

    //@Test
    public void testStress1() throws Exception {
        doStress(1000, 8, 150);
    }

    //@Test
    public  void testStress2() throws Exception {
        doStress(100, 80, 70);
    }


}
