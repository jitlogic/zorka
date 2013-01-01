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
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.*;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.jitlogic.zorka.api.SpyLib.SPD_CDISPATCHES;

/**
 * Queues incoming records resumes processing in separate thread.
 * Records are copied and only explicitly selected record attributes
 * will be copied before queueing in order to minimize strain on
 * garbage collector.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AsyncQueueCollector implements SpyProcessor, Runnable {

    /** Logger */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Record processing thread */
    private Thread thread;

    /** Record processing thread will run as long as this attribute value is true */
    private volatile boolean running;


    private volatile long submittedRecords, droppedRecords;

    /** Processing queue */
    private LinkedBlockingQueue<Map<String,Object>> procQueue = new LinkedBlockingQueue<Map<String,Object>>(1024);

    /** Records attributes to be copied */
    private final String[] attrs;

    /**
     * Standard constructor.
     *
     * @param attrs attributes to be retained when passing records to submit queue.
     */
    public AsyncQueueCollector(String... attrs) {
        this.attrs = ZorkaUtil.copyArray(attrs);
    }

    @Override
    public Map<String,Object> process(Map<String,Object> record) {

        boolean submitted = false;

        Map<String,Object> rec = ZorkaUtil.map(
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
        } catch (InterruptedException e) { }

        synchronized(this) {
            if (submitted) {
                submittedRecords++;
            } else {
                droppedRecords++;
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
    protected void doProcess(Map<String,Object> record) {

        if (record == null) {
            return;
        }

        if (SpyInstance.isDebugEnabled(SPD_CDISPATCHES)) {
            log.debug("Dispatching collector record: " + record);
        }

        SpyDefinition sdef = ((SpyContext) record.get(".CTX")).getSpyDefinition();

        for (SpyProcessor processor : sdef.getProcessors((Integer) record.get(".STAGE"))) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error transforming record: " + record + " (on processor " + processor + ")", e);
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
                doProcess(procQueue.poll(10, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) { }
        }
    }

}
