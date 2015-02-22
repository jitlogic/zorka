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
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyContext;
import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// TODO refactor this class, make its semantics similiar to spy.subchain() [but asynchronous]

/**
 * Queues incoming records resumes processing in separate thread.
 * Records are copied and only explicitly selected record attributes
 * will be copied before queueing in order to minimize strain on
 * garbage collector.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AsyncQueueCollector implements SpyProcessor, Runnable {

    /**
     * Logger
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Record processing thread
     */
    private Thread thread;

    /**
     * Record processing thread will run as long as this attribute value is true
     */
    private volatile boolean running;

    /**
     * Counts submitted records
     */
    private AtomicLong submittedRecords;

    /**
     * Counts dropped records
     */
    private AtomicLong droppedRecords;

    /**
     * Processing queue
     */
    private BlockingQueue<Map<String, Object>> procQueue = new ArrayBlockingQueue<Map<String, Object>>(128);

    /**
     * Records attributes to be copied
     */
    private final String[] attrs;

    /**
     * Standard constructor.
     *
     * @param attrs attributes to be retained when passing records to submit queue.
     */
    public AsyncQueueCollector(String... attrs) {
        this.attrs = ZorkaUtil.copyArray(attrs);
        this.submittedRecords = new AtomicLong(0);
        this.droppedRecords = new AtomicLong(0);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        boolean submitted = false;

        Map<String, Object> rec = ZorkaUtil.map(
                ".CTX", record.get(".CTX"),
                ".STAGE", record.get(".STAGE"),
                ".STAGES", record.get(".STAGES"));

        for (String attr : attrs) {
            rec.put(attr, record.get(attr));
        }

        for (Map.Entry e : record.entrySet()) {
            if (e.getKey().toString().startsWith(".")) {
                rec.put(e.getKey().toString(), e.getValue());
            }
        }

        try {
            submitted = procQueue.offer(rec, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        synchronized (this) {
            if (submitted) {
                submittedRecords.incrementAndGet();
            } else {
                droppedRecords.incrementAndGet();
            }
        }

        return record;
    }

    /**
     * This method is called from processing thread main loop to perform
     * actual processing on record obtained from queue.
     *
     * @param record record to be processed
     */
    protected void doProcess(Map<String, Object> record) {

        if (record == null) {
            return;
        }

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZSP_ARGPROC, "Dispatching collector record: " + record);
        }

        SpyDefinition sdef = ((SpyContext) record.get(".CTX")).getSpyDefinition();

        for (SpyProcessor processor : sdef.getProcessors((Integer) record.get(".STAGE"))) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Exception e) {
                log.error(ZorkaLogger.ZSP_ERRORS, "Error transforming record: " + record + " (on processor " + processor + ")", e);
                break;
            }
        }
    }

    /**
     * Starts record processing thread.
     */
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.setName("ZORKA-collect-queue-processor");
            thread.setDaemon(true);

            running = true;
            thread.start();
        }
    }


    /**
     * Stops record processing thread.
     */
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                doProcess(procQueue.take());
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Returns number of successfully submitted records.
     *
     * @return number of submitted records
     */
    public long getSubmittedRecords() {
        return submittedRecords.longValue();
    }

    /**
     * Returns number of records dropped due to queue overflow.
     *
     * @return number of dropped records
     */
    public long getDroppedRecords() {
        return droppedRecords.longValue();
    }
}
