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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.util.ZorkaLog;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.jitlogic.zorka.core.spy.SpyLib.*;

/**
 * Dispatching submitter receives submissions from probes, groups them (if needed) and sends through proper processing
 * chains defined in associated sdefs.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class DispatchingSubmitter implements SpySubmitter {

    /**
     * Logger
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Spy class transformer
     */
    private SpyClassTransformer transformer;

    /**
     * Submission stack is used to associate results from method entry probes with results from return/error probes.
     * TODO what happens to submission stack when spy context disappears when some method is executing ?
     */
    private ThreadLocal<Stack<Map<String, Object>>> submissionStack =
            new ThreadLocal<Stack<Map<String, Object>>>() {
                @Override
                public Stack<Map<String, Object>> initialValue() {
                    return new Stack<Map<String, Object>>();
                }
            };


    /**
     * Creates dispatching submitter.
     *
     * @param transformer class file transformer containing defined spy contexts
     *                    // TODO move spy context map out of class file transformer
     */
    public DispatchingSubmitter(SpyClassTransformer transformer) {
        this.transformer = transformer;
    }


    @Override
    public void submit(int stage, int id, int submitFlags, Object[] vals) {

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_SUBMIT)) {
            log.debug(ZorkaLogger.ZSP_SUBMIT, "Submitted: stage=" + stage + ", id=" + id + ", flags=" + submitFlags);
        }

        SpyContext ctx = transformer.getContext(id);

        if (ctx == null) {
            return;
        }

        Map<String, Object> record = getRecord(stage, ctx, submitFlags, vals);

        SpyDefinition sdef = ctx.getSpyDefinition();

        if (null == (record = process(stage, sdef, record))) {
            return;
        }

        if (submitFlags == SF_NONE) {
            submissionStack.get().push(record);
            return;
        }

        AgentDiagnostics.inc(AgentDiagnostics.SPY_SUBMISSIONS);

        if (sdef.getProcessors(ON_SUBMIT).size() > 0 && null == (record = process(ON_SUBMIT, sdef, record))) {
            return;
        }

    }


    /**
     * Retrieves or creates spy record for probe submission purposes.
     *
     * @param stage       method bytecode point where probe has been installed (entry, return, error)
     * @param ctx         spy context associated with submitting probe
     * @param submitFlags controls whether SUBMIT chain should be immediately processed or record should be
     *                    stored in thread local stack (and wait for another probe submission)
     * @param vals        submitted values
     * @return spy record
     */
    private Map<String, Object> getRecord(int stage, SpyContext ctx, int submitFlags, Object[] vals) {

        Map<String, Object> record;

        switch (submitFlags) {
            case SF_IMMEDIATE:
            case SF_NONE:
                record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
                break;
            case SF_FLUSH:
                Stack<Map<String, Object>> stack = submissionStack.get();
                if (stack.size() > 0) {
                    record = stack.pop();
                    // TODO check if record belongs to proper frame, warn if not
                } else {
                    log.error(ZorkaLogger.ZSP_ERRORS, "Submission thread local stack mismatch (ctx=" + ctx
                            + ", stage=" + stage + ", submitFlags=" + submitFlags + ")");
                    record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
                }
                break;
            default:
                log.error(ZorkaLogger.ZSP_ERRORS, "Illegal submission flag: " + submitFlags + ". Creating empty records.");
                record = ZorkaUtil.map(".CTX", ctx, ".STAGE", 0, ".STAGES", 0);
                break;
        }

        SpyContext context = ((SpyContext) record.get(".CTX"));
        List<SpyProbe> probes = context.getSpyDefinition().getProbes(stage);

        // TODO check if vals.length == probes.size() and log something here ...

        for (int i = 0; i < probes.size(); i++) {
            SpyProbe probe = probes.get(i);
            record.put(probe.getDstField(), vals[i]);
        }

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << stage));
        record.put(".STAGE", stage);

        return record;
    }


    /**
     * Processes specified processing chain of sdef in record
     *
     * @param stage  chain ID
     * @param sdef   spy definition with configured processing chains
     * @param record spy record (input)
     * @return spy record (output) or null if record should not be further processed
     */
    private Map<String, Object> process(int stage, SpyDefinition sdef, Map<String, Object> record) {
        List<SpyProcessor> processors = sdef.getProcessors(stage);

        record.put(".STAGES", (Integer) record.get(".STAGES") | (1 << stage));
        record.put(".STAGE", stage);

        if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
            log.debug(ZorkaLogger.ZSP_ARGPROC, "Processing records (stage=" + stage + ")");
        }

        for (SpyProcessor processor : processors) {
            try {
                if (null == (record = processor.process(record))) {
                    break;
                }
            } catch (Throwable e) {
                log.error(ZorkaLogger.ZSP_ERRORS, "Error processing record %s (on processor %s, stage=%s)", e,
                        record, processor, stage);
            }
        }

        return record;
    }

}
