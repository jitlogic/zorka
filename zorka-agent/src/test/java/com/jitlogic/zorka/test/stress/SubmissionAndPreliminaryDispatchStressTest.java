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
package com.jitlogic.zorka.test.stress;

import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.test.stress.support.StressTestFixture;

import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyProcessor;
import org.junit.Test;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.jitlogic.zorka.test.support.TestUtil.instantiate;
import static com.jitlogic.zorka.test.support.TestUtil.invoke;

public class SubmissionAndPreliminaryDispatchStressTest extends StressTestFixture {

    private static final String TCLASS3 = "com.jitlogic.zorka.test.stress.support.TestClass3";
    private static final int NUM_RERUNS = 4;
    private static final long NUM_ITERATIONS = 10000000;
    private static final int[] threadNums = { 1, 2, 4 };

    private static final String COL_SEP = "|";
    private static final int COL_LEN = 11;

    protected int verbosity = 1;

    ExecutorService executor = Executors.newFixedThreadPool(128);

    @Test
    public void testStressBasicSubmissionChainWithImmediate() throws Exception {

        final String methodName = "trivialMethod";
        final AtomicLong counter = new AtomicLong(0);

        SpyProcessor proc = new SpyProcessor() {
            @Override
            public Map<String, Object> process(Map<String, Object> record) {
                //counter.incrementAndGet();
                return record;
            }
        };

        System.out.print(""+new Date() + ": Test run ");

        SpyDefinition sdef = SpyDefinition.instance().onEnter(proc);
        List<Map<String, Object>> results = runAllStress(methodName, sdef);

        System.out.println(" DONE\n");

        printStress(System.out, results);

    }

    private void printStress(PrintStream out, List<Map<String, Object>> results) {

        Set<String> columns = results.get(0).keySet();
        StringBuilder colSb = new StringBuilder((COL_LEN+2)*columns.size());

        for (String col : columns) {
            if (colSb.length() > 0) {
                colSb.append(COL_SEP);
            }
            colSb.append(wrap(col, COL_LEN));
        }

        out.println(colSb);

        for (Map<String,Object> result : results) {
            StringBuilder sb = new StringBuilder((COL_LEN+2)*colSb.length());

            for (String col : columns) {
                if (sb.length() > 0) {
                    sb.append(COL_SEP);
                }
                sb.append(wrap(result.get(col).toString(),  COL_LEN));
            }

            out.println(sb);
        }
    }

    protected String wrap(String s, int len) {
        if (s.length() < len) {
            int lead = (len-s.length())/2;
            return spaces(lead) + s + spaces(len-s.length()-lead);
        } else {
            return s;
        }
    }

    protected String spaces(int n) {
        StringBuilder sb = new StringBuilder(n+5);
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }

    protected List<Map<String, Object>> runAllStress(String methodName, SpyDefinition sdef)

            throws Exception {
        List<Map<String,Object>> results = new ArrayList<Map<String, Object>>();

        for (int numThreads : threadNums) {
            results.add(runMultiStress(sdef, methodName, numThreads));
        }
        return results;
    }


    protected Map<String,Object> runMultiStress(SpyDefinition sdef, final String methodName,
        int numThreads) throws Exception {

        List<Map<String,Long>> data = new ArrayList<Map<String, Long>>(NUM_RERUNS+2);

        if (verbosity >= 2) {
            System.out.print("" + new Date() + " Running " + methodName
                + " (iters=" + NUM_ITERATIONS + ", threads=" + numThreads + ") ");
        }

        for (int i = 0; i < NUM_RERUNS; i++) {
            if (verbosity >= 1) {
                System.out.print("."); System.out.flush();
            }
            data.add(runStress(sdef, methodName, numThreads));
        }

        if (verbosity >= 2) {
            System.out.println(" DONE");
        }

        Map<String,Object> result = new LinkedHashMap<String, Object>();
        result.put("THREADS", numThreads);

        for (String attr : data.get(0).keySet()) {
            String fmin = "MIN(" + attr + ")", fmax =  "MAX(" + attr + ")", favg = attr;
            long tmin = Long.MAX_VALUE,  tmax = Long.MIN_VALUE, tavg = 0L;

            for (Map<String,Long> m : data) {
                long t = m.get(attr);
                tmin = t < tmin ? t : tmin;
                tmax = t > tmax ? t : tmax;
                tavg += t;
            }

            result.put(favg, tavg/data.size());
            result.put(fmin, tmin);
            result.put(fmax, tmax);
        }

        return result;
    }


    protected Map<String,Long> runStress(SpyDefinition sdef, final String methodName, int numThreads) throws Exception {

        spyTransformer.reset();
        spyInstance.add(sdef.include(spy.byMethod(TCLASS3, methodName)));

        final Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS3);
        final Map<Integer,Long> times = new ConcurrentHashMap<Integer, Long>();
        final CountDownLatch allThreadsFinished = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int thread = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    long t1 = System.currentTimeMillis();
                    try {
                        invoke(obj, "run", methodName, NUM_ITERATIONS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    long t2 = System.currentTimeMillis();
                    times.put(thread, t2-t1);
                    allThreadsFinished.countDown();
                }
            });
        }

        long t1 = System.currentTimeMillis();

        allThreadsFinished.await();

        long t2 = System.currentTimeMillis();

        long tmin = Long.MAX_VALUE, tmax = Long.MIN_VALUE, tavg = 0;

        for (Long t : times.values()) {
            tmin = t < tmin ? t : tmin;
            tmax = t > tmax ? t : tmax;
            tavg += t;
        }


        Map<String,Long> result = new LinkedHashMap<String, Long>();
        result.put("TAVG", tavg/times.size());
        result.put("TMIN", tmin);
        result.put("TMAX", tmax);
        //result.put("TUSR", (t2-t1));

        return result;
    }

}
