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

import com.jitlogic.zorka.spy.collectors.AsyncQueueCollector;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.spy.probes.SpyProbe;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.jitlogic.zorka.api.SpyLib.*;

/**
 *
 */
public class DispatchingSubmitter implements SpySubmitter {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SpyClassTransformer engine;
    private SpyProcessor collector;

    private ThreadLocal<Stack<Map<String,Object>>> submissionStack =
        new ThreadLocal<Stack<Map<String,Object>>>() {
            @Override
            public Stack<Map<String,Object>> initialValue() {
                return new Stack<Map<String,Object>>();
            }
        };


    public DispatchingSubmitter(SpyClassTransformer engine, SpyProcessor collector) {
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

        Map<String,Object> record = getRecord(stage, ctx, submitFlags, vals);

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

    }


    private Map<String,Object> getRecord(int stage, SpyContext ctx, int submitFlags, Object[] vals) {

        Map<String,Object> record = null;

        switch (submitFlags) {
            case SF_IMMEDIATE:
            case SF_NONE:
                record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
                break;
            case SF_FLUSH:
                Stack<Map<String,Object>> stack = submissionStack.get();
                if (stack.size() > 0) {
                    record = stack.pop();
                    // TODO check if record belongs to proper frame, warn if not
                } // TODO warn if there was no record on stack
        }

        List<SpyProbe> probes = ((SpyContext)record.get(".CTX")).getSpyDefinition().getProbes(stage);

        // TODO check if vals.length == probes.size() and log something here ...

        for (int i = 0; i < probes.size(); i++) {
            SpyProbe probe = probes.get(i);
            record.put(probe.getKey(), probe.processVal(vals[i]));
        }

        record.put(".STAGES", (Integer)record.get(".STAGES") | (1 << stage));
        record.put(".STAGE", stage);

        return record;
    }

    private Map<String,Object> process(int stage, SpyDefinition sdef, Map<String,Object> record) {
        List<SpyProcessor> processors = sdef.getProcessors(stage);

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << stage));
        record.put(".STAGE", stage);

        if (SpyInstance.isDebugEnabled(SPD_ARGPROC)) {
            log.debug("Processing records (stage=" + stage + ")");
        }

        for (SpyProcessor processor : processors) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error processing record " + record + " (on processor "
                            + processor + ", stage=" + stage + ")");
            }
        }

        return record;
    }

    public void setCollector(AsyncQueueCollector collector) {
        if (collector != null) {
            collector.stop();
        }
        this.collector = collector;
    }

    public void start() {
        if (collector instanceof AsyncQueueCollector) {
            ((AsyncQueueCollector)collector).start();
        }
    }

    public void stop() {
        if (collector instanceof AsyncQueueCollector) {
            ((AsyncQueueCollector)collector).stop();
        }
    }
}
