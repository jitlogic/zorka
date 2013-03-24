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
package com.jitlogic.zorka.agent.test.stress.support;

import com.jitlogic.zorka.agent.spy.SpyDefinition;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.jitlogic.zorka.agent.test.support.TestUtil.instantiate;
import static com.jitlogic.zorka.agent.test.support.TestUtil.invoke;

public class StressTestFixture extends ZorkaFixture {

    private static final String TCLASS3 = "com.jitlogic.zorka.agent.test.stress.support.TestClass3";

    protected int NUM_RERUNS = 8;
    protected long NUM_ITERATIONS = 10000000;
    protected int[] NUM_THREADS = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 1024, 2048, 4096 };
    protected int MAX_THREADS = 64;

    protected String COL_SEP = "|";
    protected int COL_LEN = 11;

    protected int verbosity = 1;
    private ExecutorService executor;

    protected void printStress(PrintStream out, List<Map<String, Object>> results) {

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


    protected void runBenchmark(String methodName, SpyDefinition sdef,
                                PrintStream out, String description) throws Exception {
        out.print(""+new Date() + ": " + description + " ");

        List<Map<String, Object>> results = runAllStress(methodName, sdef);

        out.println(" DONE " + new Date() + "\n");

        printStress(out, results);
    }


    protected List<Map<String, Object>> runAllStress(String methodName, SpyDefinition sdef) throws Exception {
        List<Map<String,Object>> results = new ArrayList<Map<String, Object>>();

        for (int numThreads : NUM_THREADS) {
            if (numThreads <= MAX_THREADS) {
                results.add(runMultiStress(sdef, methodName, numThreads));
            }
        }
        return results;
    }

    protected Map<String,Object> runMultiStress(SpyDefinition sdef, final String methodName, int numThreads) throws Exception {

        if (executor == null) {
            executor = Executors.newFixedThreadPool(MAX_THREADS);
        }

        List<Map<String,Long>> data = new ArrayList<Map<String, Long>>(NUM_RERUNS+2);

        if (verbosity >= 2) {
            System.out.print("" + new Date() + " Running " + methodName
                + " (iters=" + NUM_ITERATIONS + ", threads=" + numThreads + ") ");
        } else if (verbosity >= 1) {
            System.out.print(" " + numThreads + " ");
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
            String fmin = "MIN(" + attr + ")", fmax =  "MAX(" + attr + ")";
            long tmin = Long.MAX_VALUE,  tmax = Long.MIN_VALUE, tavg = 0L;

            for (Map<String,Long> m : data) {
                long t = m.get(attr);
                tmin = t < tmin ? t : tmin;
                tmax = t > tmax ? t : tmax;
                tavg += t;
            }

            result.put(attr, tavg/data.size());
            result.put("T("+attr+")", 1000000L*tavg/(NUM_ITERATIONS*numThreads*data.size()));
            result.put(fmin, tmin);
            result.put(fmax, tmax);
        }

        return result;
    }

    protected Map<String,Long> runStress(SpyDefinition sdef, final String methodName, int numThreads) throws Exception {

        if (executor == null) {
            executor = Executors.newFixedThreadPool(numThreads > MAX_THREADS ? numThreads : MAX_THREADS);
        }

        spyTransformer.reset();
        spyInstance.getClassTransformer().add(sdef.include(spy.byMethod(TCLASS3, methodName)));

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
        result.put("TUSR", (t2-t1));

        return result;
    }

}
