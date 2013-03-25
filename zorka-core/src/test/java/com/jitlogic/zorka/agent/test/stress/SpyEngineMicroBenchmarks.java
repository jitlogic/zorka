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
package com.jitlogic.zorka.agent.test.stress;

import com.jitlogic.zorka.agent.test.stress.support.StressTestFixture;

import com.jitlogic.zorka.agent.spy.SpyDefinition;
import com.jitlogic.zorka.agent.spy.SpyProcessor;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SpyEngineMicroBenchmarks extends StressTestFixture {

    private SpyProcessor nullProcessor = new SpyProcessor() {
        @Override
        public Map<String, Object> process(Map<String, Object> record) {
            return record;
        }
    };


    private AtomicLong counter = new AtomicLong(0);

    private SpyProcessor counterProcessor = new SpyProcessor() {
        @Override
        public Map<String, Object> process(Map<String, Object> record) {
            counter.incrementAndGet();
            return record;
        }
    };


    private long synCounter = 0L;

    private SpyProcessor synchronizedCounterProcessor = new SpyProcessor() {
        @Override
        public synchronized Map<String, Object> process(Map<String, Object> record) {
            synCounter++;
            return record;
        }
    };


    private AtomicLong tstamp = new AtomicLong(0);

    private SpyProcessor lastTstampProcessor = new SpyProcessor() {
        @Override
        public Map<String, Object> process(Map<String, Object> record) {
            tstamp.set((Long)record.get("T"));
            return record;
        }
    };


    @Test
    public void benchmarkInstrumentationWithNullProcessor() throws Exception {

        SpyDefinition sdef = spy.instance().onEnter(nullProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Null processor on method entry");
    }


    @Test
    public void benchmarkInstrumentationWithSingleSyncOperation() throws Exception {

        SpyDefinition sdef = spy.instance().onEnter(counterProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Single atomic  operation on method entry");
    }

    @Test
    public void benchmarkInstrumentationWithCallingSynchronizedMethod() throws Exception {

        SpyDefinition sdef = spy.instance().onEnter(synchronizedCounterProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Synchronized method call");

        System.out.println("\nCounter = " + synCounter);
    }


    @Test
    public void benchmarkSimpleArgumentFetch() throws Exception {

        SpyDefinition sdef = spy.instance().onEnter(spy.fetchArg("THIS", 0), nullProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Simple argument fetch");
    }


    @Test
    public void benchmarkSingleTimestampFetch() throws Exception {

        SpyDefinition sdef = spy.instance().onEnter(spy.fetchTime("T"), nullProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Simple argument fetch");
    }


    @Test
    public void benchmarkFullExecutionTimeCalculation() throws Exception {
        SpyDefinition sdef = spy.instrument().onEnter(nullProcessor);

        runBenchmark("trivialMethod", sdef, System.out, "Full execution time calculation");
    }


    @Test
    public void benchmarkFullExecutionTimeCalculationWithZorkaStatsCollector() throws Exception {
        SpyDefinition sdef = spy.instrument().onSubmit(
                spy.zorkaStats("java", "zorka:type=ZorkaStats,name=Benchmark1", "stats", "trivialMethod", "T"));

        //MAX_THREADS = 1; // TODO performance issue in MethodCallStats code (propably aggregate value calculation)

        runBenchmark("trivialMethod", sdef, System.out, "Full execution time calculation");
    }
}
