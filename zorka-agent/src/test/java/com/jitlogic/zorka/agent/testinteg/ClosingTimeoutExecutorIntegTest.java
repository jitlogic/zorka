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

package com.jitlogic.zorka.agent.testinteg;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.agent.testutil.TestLogger;
import com.jitlogic.zorka.util.ClosingTimeoutExecutor;
import com.jitlogic.zorka.util.ZorkaLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ClosingTimeoutExecutorIntegTest {


    public static class TestTask implements Runnable, Closeable {


        private volatile int runs = 0, closes = 0;
        private volatile long sleepTime;

        public TestTask(long sleepTime) {
            this.sleepTime = sleepTime;
        }

        public void close() throws IOException {
            closes++;
        }

        public void run() {
            if (sleepTime > 0) {
                try {
                    //System.out.println("Sleeping for " + sleepTime);
                    Thread.sleep(sleepTime);
                    //System.out.println("go !");
                    runs++;
                } catch (InterruptedException e) {
                    //System.out.println("Thread interrupted !");
                    closes++;
                }
            } else
                while (true) ;
        }

        public int getRuns() {
            return runs;
        }

        public int getCloses() {
            return closes;
        }
    }

    @Before
    public void setUp() {
        ZorkaConfig.loadProperties(this.getClass().getResource("/conf").getPath());
        ZorkaLogger.setLogger(new TestLogger());
    }

    @After
    public void tearDown() {
        ZorkaLogger.setLogger(null);
        ZorkaConfig.cleanup();;
    }

    @Test
    public void testImmediateTaskExecution() throws Exception {
        ClosingTimeoutExecutor executor = new ClosingTimeoutExecutor(2, 2, 1000);
        TestTask task =  new TestTask(1);
        executor.execute(task);
        Thread.sleep(20);
        assertEquals(1, task.getRuns());
    }


    @Test
    public void testTimeoutTaskExecutionWithCheck() throws Exception {
        ClosingTimeoutExecutor executor = new ClosingTimeoutExecutor(2, 2, 30);
        TestTask task =  new TestTask(45);
        executor.execute(task);
        Thread.sleep(20);
        assertEquals(0, task.getRuns());
        assertEquals(0, task.getCloses());
        Thread.sleep(30);
        assertEquals(0, task.getRuns());
        assertEquals(1, task.getCloses());
    }


    @Test
    public void testTimeoutTaskExecutionWithoutCheck() throws Exception {
        ClosingTimeoutExecutor executor = new ClosingTimeoutExecutor(2, 2, 30);
        TestTask task =  new TestTask(-1);
        executor.execute(task);
        Thread.sleep(20);
        assertEquals(0, task.getRuns());
        assertEquals(0, task.getCloses());
        Thread.sleep(30);
        assertEquals(0, task.getRuns());
        assertEquals(1, task.getCloses());
    }


    @Test
    public void testQueueOverflow() throws Exception {
        // TODO blinking test !!!
        ClosingTimeoutExecutor executor = new ClosingTimeoutExecutor(2, 2, 500);
        List<TestTask> tasks = new ArrayList<TestTask>(6);
        for (int i = 1; i <= 6; i++)
            tasks.add(new TestTask(i*200));

        for (TestTask t : tasks)
            executor.execute(t);

        Thread.sleep(10);

        assertEquals(0, tasks.get(0).getCloses());
        assertEquals(0, tasks.get(1).getCloses());
        assertEquals(1, tasks.get(4).getCloses());
        //assertEquals(1, tasks.get(5).getCloses());

    }
}
