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
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.jitlogic.zorka.spy.SpyLib.SPD_CDISPATCHES;

public class CollectQueueProcessor implements SpyProcessor, Runnable {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private volatile Thread thread;
    private volatile boolean running;

    private volatile long submittedRecords, droppedRecords;

    private LinkedBlockingQueue<SpyRecord> procQueue = new LinkedBlockingQueue<SpyRecord>(1024);

    private String[] attrs;


    public CollectQueueProcessor(String...attrs) {
        this.attrs = ZorkaUtil.copyArray(attrs);
    }


    public SpyRecord process(SpyRecord record) {

        boolean submitted = false;

        SpyRecord rec = new SpyRecord(record, attrs);

        try {
                submitted = procQueue.offer(rec, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) { }

        if (submitted) {
            submittedRecords++;
        } else {
            droppedRecords++;
        }

        return record;
    }


    protected void doProcess(SpyRecord record) {

        if (record == null) {
            return;
        }

        if (SpyInstance.isDebugEnabled(SPD_CDISPATCHES)) {
            log.debug("Dispatching collector record: " + record);
        }

        SpyDefinition sdef = record.getContext().getSpyDefinition();

        for (SpyProcessor processor : sdef.getProcessors(record.getStage())) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Throwable e) {
                log.error("Error transforming record: " + record + " (on processor " + processor + ")", e);
                break;
            }
        }
    }


    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.setName("ZORKA-collect-queue-processor");
            thread.setDaemon(true);

            running = true;
            thread.start();
        }
    }


    public void stop() {
        running = false;
    }


    public void run() {
        while (running) {
            try {
                doProcess(procQueue.poll(10, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) { }
        }
    }

}
