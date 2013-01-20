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

package com.jitlogic.zorka.test.stress;

import com.jitlogic.zorka.agent.spy.MainSubmitter;
import com.jitlogic.zorka.agent.spy.SpyDefinition;
import com.jitlogic.zorka.agent.spy.SpySubmitter;
import com.jitlogic.zorka.test.stress.support.StressTestFixture;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Set of microbenchmarks measuring impact of various (potential) submitter implemementations.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SubmitPhaseMicrobenchmarks extends StressTestFixture {
    /**
     * This submitter does not store (almost) anything, yet some code is required to
     * make sure JIT won't optimize probes away. For some reason it still takes (a bit of) time
     * starting with two-thread run, propably due to false sharing.
     */
    private SpySubmitter nullSubmitter = new SpySubmitter() {
        int count = 0;
        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            // Quick hack to ensure instrumented code won't be optimized away
            int i = stage + id + submitFlags + (vals != null ? vals.length : 0);
            if (i % 99999 == 0) {
                count += i;
            }
        }
    };


    @Test
    public void testNullSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0));

        MainSubmitter.setSubmitter(unsynchronizedSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Null submitter and argument probe");

        System.out.println("Submitter_errors=" + MainSubmitter.getErrorCount());
    }



    /**
     * Performs non-thread-safe operation but in this benchmark we don't really care about
     * results. Yet it exposes that false sharing can incur performance penalty regardless
     * of (non)synchronized nature of reads/writes.
     */
    private SpySubmitter unsynchronizedSubmitter = new SpySubmitter() {
        long counter;
        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            // Quick hack to ensure instrumented code won't be optimized away
            int i = stage + id + submitFlags + (vals != null ? vals.length : 0);
            counter += i;
        }
    };


    @Test
    public void testUnsynchronizedSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0));

        MainSubmitter.setSubmitter(unsynchronizedSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Unsynchronized submitter and argument probe");

        System.out.println("errors=" + MainSubmitter.getErrorCount());
    }


    class Counter {
        private long cnt = 0;

        public void add(long i) {
            cnt += i;
        }

        public void set(long i) {
            cnt = i;
        }

        public long getCnt() {
            return cnt;
        }
    }

    /**
     * This submitter keeps data in ThreadLocal. It seems that thread local is faster than
     * unsynchronized access to common variable (false sharing once again).
     */
    private SpySubmitter threadLocalSubmitter = new SpySubmitter() {

        ThreadLocal<Counter> tlCounters = new ThreadLocal<Counter>() {
            public Counter initialValue() {
                return new Counter();
            }
        };

        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            // Quick hack to ensure instrumented code won't be optimized away
            int i = stage + id + submitFlags + (vals != null ? vals.length : 0);
            tlCounters.get().add(i);
        }
    };


    @Test
    public void benchThreadLocalSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0));

        MainSubmitter.setSubmitter(threadLocalSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Thread local submitter and argument probe");

        System.out.println("Submitter errors: " + MainSubmitter.getErrorCount());
    }


    private AtomicLong tlaCounter = new AtomicLong(0);

    /**
     * Another version of thread-local submitter that eliminates possibility for
     * compiler optimizing something away.
     */
    private SpySubmitter threadLocalSemiAtomicSubmitter = new SpySubmitter() {

        ThreadLocal<Counter> tlCounters = new ThreadLocal<Counter>() {
            public Counter initialValue() {
                return new Counter();
            }
        };

        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            int i = stage + id + submitFlags + (vals != null ? vals.length : 0);
            Counter cnt = tlCounters.get();
            cnt.add(i);

            if (cnt.getCnt() > 1000000) {
                tlaCounter.incrementAndGet();
                cnt.set(0);
            }
        }
    };

    @Test
    public void testThreadLocalSemiAtomicSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0));

        MainSubmitter.setSubmitter(threadLocalSemiAtomicSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Thread local semi-atomic submitter and argument probe");

        System.out.println("Submitter errors: " + MainSubmitter.getErrorCount());
        System.out.println("Final counter: " + tlaCounter.longValue());
    }


    /**
     * Collects timestamps (seems work pretty fast and scale pretty well).
     */
    private SpySubmitter threadLocalTstampSubmitter = new SpySubmitter() {

        ThreadLocal<Counter> tlCounters = new ThreadLocal<Counter>() {
            public Counter initialValue() {
                return new Counter();
            }
        };

        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            long t = (Long)vals[0];
            long i = stage + id + submitFlags + (t%5);
            Counter cnt = tlCounters.get();
            cnt.add(i);
        }
    };

    @Test
    public void testThreadLocalTstampSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchTime("T"));

        MainSubmitter.setSubmitter(threadLocalSemiAtomicSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Thread local submitter and timestamp probe");

        System.out.println("Submitter errors: " + MainSubmitter.getErrorCount());
        System.out.println("Final counter: " + tlaCounter.longValue());
    }

    private SpySubmitter atomicSubmitter = new SpySubmitter() {
        AtomicLong counter = new AtomicLong(0);
        @Override
        public void submit(int stage, int id, int submitFlags, Object[] vals) {
            // Quick hack to ensure instrumented code won't be optimized away
            int i = stage + id + submitFlags + (vals != null ? vals.length : 0);
            counter.addAndGet(i);
        }
    };

    @Test
    public void testInstrumentationWithAtomicSubmitter() throws Exception {
        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0));

        MainSubmitter.setSubmitter(atomicSubmitter);

        runBenchmark("trivialMethod", sdef, System.out, "Atomic submitter and argument probe");

        System.out.println("errors=" + MainSubmitter.getErrorCount());
    }


}
