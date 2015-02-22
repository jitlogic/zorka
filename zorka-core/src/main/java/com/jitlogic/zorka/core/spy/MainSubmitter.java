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

import bsh.EvalError;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

/**
 * Main submitter contains static methods that can be called directly by
 * instrumentation probes. It forwards requests to actual submitter that
 * can be configured using setSubmitter() method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MainSubmitter {

    private static final ZorkaLog log = ZorkaLogger.getLog(MainSubmitter.class);

    /**
     * Submitter receiving full submissions
     */
    private static SpySubmitter submitter;

    /**
     * Tracer receiving trace events
     */
    private static Tracer tracer;

    /**
     * Thread local
     */
    private static ThreadLocal<Boolean> inSubmit = new ThreadLocal<Boolean>() {
        public Boolean initialValue() {
            return false;
        }
    };

    /**
     * This method is called by spy probes.
     *
     * @param stage       entry, return point or error handling point of spy probe
     * @param id          spy context ID
     * @param submitFlags submit flags
     * @param vals        values fetched by probe
     */
    public static void submit(int stage, int id, int submitFlags, Object[] vals) {

        if (inSubmit.get()) {
            return;
        }

        try {
            tracer.getHandler().disable();
            if (submitter != null) {
                inSubmit.set(true);
                submitter.submit(stage, id, submitFlags, vals);
            }
        } catch (EvalError e) {
            log.debug(ZorkaLogger.ZSP_ERRORS, "Error submitting value from instrumented code: ", e);
            AgentDiagnostics.inc(AgentDiagnostics.SPY_ERRORS);
        } catch (Throwable e) {
            log.debug(ZorkaLogger.ZSP_ERRORS, "Error submitting value from instrumented code: ", e);
            AgentDiagnostics.inc(AgentDiagnostics.SPY_ERRORS);
        } finally {
            inSubmit.set(false);
            tracer.getHandler().enable();
        }
    }


    /**
     * This method is called by tracer probes at method start.
     *
     * @param classId  class ID (registered)
     * @param methodId method ID (registered)
     */
    public static void traceEnter(int classId, int methodId, int signatureId) {

        if (tracer != null) {
            try {
                tracer.getHandler().traceEnter(classId, methodId, signatureId, System.nanoTime());
            } catch (Throwable e) {
                log.debug(ZorkaLogger.ZTR_TRACE_ERRORS, "Error executing traceEnter", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (ZorkaLogger.isLogMask(ZorkaLogger.ZTR_TRACER_DBG)) {
            log.warn(ZorkaLogger.ZTR_TRACE_CALLS, "Submitter is null !");
        }

    }


    /**
     * This method is called by tracer probes at method exit.
     */
    public static void traceReturn() {

        if (tracer != null) {
            try {
                tracer.getHandler().traceReturn(System.nanoTime());
            } catch (Throwable e) {
                log.debug(ZorkaLogger.ZTR_TRACE_ERRORS, "Error executing traceReturn", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (ZorkaLogger.isLogMask(ZorkaLogger.ZTR_TRACER_DBG)) {
            log.warn(ZorkaLogger.ZTR_TRACE_CALLS, "Submitter is null !");
        }

    }


    /**
     * This method is called by tracer probes at method error point.
     *
     * @param exception exception thrown
     */
    public static void traceError(Throwable exception) {

        if (tracer != null) {
            try {
                tracer.getHandler().traceError(exception, System.nanoTime());
            } catch (Throwable e) {
                log.debug(ZorkaLogger.ZTR_TRACE_ERRORS, "Error executing traceError", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (ZorkaLogger.isLogMask(ZorkaLogger.ZTR_TRACER_DBG)) {
            log.warn(ZorkaLogger.ZTR_TRACE_CALLS, "Submitter is null !");
        }
    }

    /**
     * Sets backing spy submitter
     *
     * @param submitter spy submitter
     */
    public static void setSubmitter(SpySubmitter submitter) {
        MainSubmitter.submitter = submitter;
    }


    /**
     * Sets backing trace event handler.
     *
     * @param tracer
     */
    public static void setTracer(Tracer tracer) {
        MainSubmitter.tracer = tracer;
    }

}
