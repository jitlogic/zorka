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
package com.jitlogic.zorka.test.spy;

import com.jitlogic.zorka.test.spy.support.StressTestFixture;

import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyProcessor;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.jitlogic.zorka.test.spy.BytecodeInstrumentationUnitTest.TCLASS1;
import static com.jitlogic.zorka.test.util.JmxTestUtil.instantiate;
import static com.jitlogic.zorka.test.util.JmxTestUtil.invoke;

public class SubmissionAndPreliminaryDispatchStressTest extends StressTestFixture {


    @Test
    public void testStressBasicSubmissionChainWithImmediate() throws Exception {

        final int NUM_THREADS = 4;
        final long NUM_ITERS = 10000000;

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final AtomicLong counter = new AtomicLong(0);

        final CountDownLatch allThreadsFinished = new CountDownLatch(NUM_THREADS);

        SpyProcessor proc = new SpyProcessor() {
            @Override
            public Map<String, Object> process(Map<String, Object> record) {
                counter.incrementAndGet();
                return record;
            }
        };

        SpyDefinition sdef = SpyDefinition.instance().onEnter(proc).include(spy.byMethod(TCLASS1, "trivialMethod"));
        spyInstance.add(sdef);

        final Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);

        System.out.println("Starting threads ...");


        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    for (long i = 0; i < NUM_ITERS; i++) {
                        try {
                            invoke(obj, "trivialMethod");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    allThreadsFinished.countDown();
                }
            });
        }

        System.out.println("Waiting for threads to finish ...");
        long t1 = System.currentTimeMillis();

        allThreadsFinished.await();

        long t2 = System.currentTimeMillis();


        System.out.println("Threads finished: t=" + (t2-t1));
        System.out.println("Submitted values: " + counter.longValue());
    }

}
