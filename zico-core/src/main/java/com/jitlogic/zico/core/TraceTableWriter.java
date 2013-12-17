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
package com.jitlogic.zico.core;


import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


@Singleton
public class TraceTableWriter implements Runnable {

    private static Logger log = LoggerFactory.getLogger(TraceTableWriter.class);

    private BlockingQueue<Map<String, Object>> submitQueue;

    private int batchSize;
    private int retries;
    private long submitTimeout;
    private boolean synchronous;

    private SimpleJdbcInsert jdbci;

    private Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(false);


    @Inject
    public TraceTableWriter(DataSource ds, ZicoConfig config) {
        submitQueue = new ArrayBlockingQueue<Map<String, Object>>(config.intCfg("dbwriter.queue.size", 1024));
        batchSize = config.intCfg("dbwriter.batch.size", 64);
        retries = config.intCfg("dbwriter.retry.count", 5);
        submitTimeout = config.longCfg("dbwriter.submit.timeout", 30000L);
        synchronous = config.boolCfg("dbwriter.synchronous.mode", false);

        jdbci = new SimpleJdbcInsert(ds).withTableName("TRACES").usingColumns(
                "HOST_ID", "DATA_OFFS", "INDEX_OFFS", "INDEX_LEN", "TRACE_ID", "DATA_LEN", "CLOCK", "RFLAGS", "TFLAGS", "STATUS",
                "CLASS_ID", "METHOD_ID", "SIGN_ID", "CALLS", "ERRORS", "RECORDS", "EXTIME", "ATTRS", "EXINFO");
    }


    public void submit(Map<String, Object> rec) {
        if (synchronous) {
            jdbci.execute(rec);
        } else {
            try {

                if (!submitQueue.offer(rec, submitTimeout, TimeUnit.MILLISECONDS)) {
                    log.error("Timeout waiting for trace record submission in DBWriter (host=" + rec.get("HOST_ID")
                            + ", traceOffs=" + rec.get("DATA_OFFS") + ", length=" + rec.get("DATA_LEN") + ")");
                }
            } catch (InterruptedException e) {

            }
        }
    }


    private List<Map<String, Object>> save(List<Map<String, Object>> records) {
        List<Map<String, Object>> dropped = new ArrayList<Map<String, Object>>();

        log.debug("Saving " + records.size() + " records in trace table.");

        for (Map<String, Object> m : records) {
            try {
                jdbci.execute(m);
            } catch (Exception e) {
                log.error("Error inserting record to TRACES table", e);
                dropped.add(m);
            }
        }

        return dropped;
    }


    private void runCycle(int retries, boolean wait) {
        try {
            List<Map<String, Object>> recs = new ArrayList<Map<String, Object>>(batchSize);
            if (wait) {
                Map<String, Object> rec = submitQueue.take();
                recs.add(rec);
            }
            if (batchSize > 1) {
                submitQueue.drainTo(recs, batchSize - 1);
            }

            for (int i = 0; recs.size() > 0 && i < retries; i++) {
                recs = save(recs);
            }

            if (recs.size() > 0) {
                log.error("Not all records have been saved correctly. ");
            }

        } catch (InterruptedException e) {

        } catch (Exception e) {
            log.error("Error in trace table writer", e);
        }
    }


    @Override
    public void run() {

        log.info("Trace table writer has started.");

        while (running.get()) {
            runCycle(retries, true);
        }

        log.info("Trace table writer is stopping ...");

        while (submitQueue.size() > 0) {
            runCycle(retries, false);
        }

        log.info("Trace table writer has stopped.");
    }


    public synchronized void start() {
        if (!synchronous && thread == null) {
            log.info("Trace table writer is starting ...");
            thread = new Thread(this);
            thread.setName("ZICO-db-writer");
            thread.setDaemon(true);
            running.set(true);
            thread.start();
        } else {
            log.info("Trace table writer startup has been skipped.");
        }
    }


    public synchronized void stop() {
        running.set(false);
        long t = System.currentTimeMillis();

        log.info("Waiting for trace table writer to finish ...");

        while (!synchronous && submitQueue.size() > 0 && System.currentTimeMillis() - t > 60000) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

}
