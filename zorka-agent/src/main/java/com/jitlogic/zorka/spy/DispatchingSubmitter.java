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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.spy.processors.CollectQueueProcessor;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.List;
import java.util.Stack;

import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 *
 */
public class DispatchingSubmitter implements SpySubmitter {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SpyClassTransformer engine;
    private CollectQueueProcessor collector;

    private ThreadLocal<Stack<SpyRecord>> submissionStack =
        new ThreadLocal<Stack<SpyRecord>>() {
            @Override
            public Stack<SpyRecord> initialValue() {
                return new Stack<SpyRecord>();
            }
        };


    public DispatchingSubmitter(SpyClassTransformer engine, CollectQueueProcessor collector) {
        this.engine = engine;
        this.collector = collector;
    }


    public void submit(int stage, int id, int submitFlags, Object[] vals) {

        if (SpyInstance.isDebugEnabled(SPD_SUBMISSIONS)) {
            log.debug("Submitted: stage=" + stage + ", id=" + id + ", flags=" + submitFlags);
        }

        SpyContext ctx = engine.getContext(id);

        if (ctx == null) {
            return;
        }

        SpyRecord record = getRecord(stage, ctx, submitFlags, vals);

        SpyDefinition sdef = ctx.getSpyDefinition();

        if (null == (record = process(stage, sdef, record))) {
            return;
        }

        if (submitFlags == SF_NONE) {
            submissionStack.get().push(record);
            return;
        }


        if (sdef.getProcessors(ON_SUBMIT).size() > 0 && null == (record = process(ON_SUBMIT, sdef, record))) {
            return;
        }

        //record.cleanup();

        if (sdef.getProcessors(SpyLib.ON_COLLECT).size() > 0) {
            collector.process(SpyLib.ON_COLLECT, record);
        }
    }


    private SpyRecord getRecord(int stage, SpyContext ctx, int submitFlags, Object[] vals) {

        SpyRecord record = null;

        switch (submitFlags) {
            case SF_IMMEDIATE:
            case SF_NONE:
                record = new SpyRecord(ctx);
                break;
            case SF_FLUSH:
                Stack<SpyRecord> stack = submissionStack.get();
                if (stack.size() > 0) {
                    record = stack.pop();
                    // TODO check if record belongs to proper frame, warn if not
                } // TODO warn if there was no record on stack
        }

        record.feed(stage, vals);

        return record;
    }

    private SpyRecord process(int stage, SpyDefinition sdef, SpyRecord record) {
        List<SpyProcessor> processors = sdef.getProcessors(stage);

        if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
            log.debug("Processing records (stage=" + stage + ")");
        }

        for (SpyProcessor processor : processors) {
            try {
                if (null == (record = processor.process(stage, record))) {
                    break;
                }
            } catch (Throwable e) {
                log.error("Error processing record " + record + " (on processor "
                            + processor + ", stage=" + stage + ")");
            }
        }

        return record;
    }

    public void setCollector(CollectQueueProcessor collector) {
        if (collector != null) {
            collector.stop();
        }
        this.collector = collector;
    }

    public void start() {
        collector.start();
    }

    public void stop() {
        collector.stop();
    }
}
