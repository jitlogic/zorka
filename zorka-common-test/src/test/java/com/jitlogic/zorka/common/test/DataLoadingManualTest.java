/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.test;


import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoDataLoader;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DataLoadingManualTest {

    // TODO move this thing to stress tests

    private AtomicLong records = new AtomicLong(0), bytes = new AtomicLong(0);
    private AtomicInteger errors = new AtomicInteger(0), passes = new AtomicInteger(0);

    int submissions = 0;

    private Executor executor = Executors.newFixedThreadPool(12);

    private void load(File dir, String file, String hostname) {
        System.out.println("Starting: " + new File(dir, file));
        long t1 = System.nanoTime();
        try {
            ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, hostname, "");
            loader.load(new File(dir, file).getPath());

            records.addAndGet(loader.getRecords());
            bytes.addAndGet(loader.getBytes());

            long t = (System.nanoTime()-t1)/1000000;
            long recsps = 1000L * loader.getRecords() / t;
            long bytesps = 1000L * loader.getBytes() / t;

            System.out.println("File " + dir + "/" + file + " finished: t=" + t
                    + " records=" + loader.getRecords() + " (" + recsps + " recs/s)"
                    + " bytes=" + loader.getBytes() + "(" + bytesps + " bytes/s).");

        } catch (Exception e) {
            errors.incrementAndGet();
        }
        passes.incrementAndGet();
    }

    //@Test
    public void testLoadDataFile() throws Exception {
        ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, System.getProperty("load.host", "test"), "");
        loader.load(System.getProperty("load.file", "/tmp/trace.ztr"));
    }

    private Set<String> VERBOTEN = ZorkaUtil.set(".", "..");

    //@Test @Ignore
    public void testLoadMultipleDataFiles() throws Exception {
        File rootdir = new File("/tmp/traces");
        for (final String d : rootdir.list()) {
            final File dir = new File(rootdir, d);
            if (!VERBOTEN.contains(d) && dir.isDirectory()) {
                for (final String f : dir.list()) {
                    if (f.matches("^trace.ztr.*")) {
                        System.out.println("Submitting: " + new File(dir, f));
                        submissions++;
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                load(dir, f, d+f);
                            }
                        });
                    }
                }
            }
        }

        long t1 = System.nanoTime();
        while (submissions > passes.get()) {
            Thread.sleep(500);
        }
        long t = (System.nanoTime()-t1)/1000000;
        long recsps = 1000L * records.get() / t;
        long bytesps = 1000L * bytes.get() / t;

        System.out.println("Overall execution time: " + t + "ms");
        System.out.println("Overall Records processed: " + records.get() + "(" + recsps + " recs/s)");
        System.out.println("Overall Bytes processed: " + bytes.get() + "(" + bytesps + " bytes/s");
    }

}
