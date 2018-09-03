/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.spy.lt.LTracer;
import com.jitlogic.zorka.core.spy.st.STracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main submitter contains static methods that can be called directly by
 * instrumentation probes. It forwards requests to actual submitter that
 * can be configured using setSubmitter() method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MainSubmitter {

    private static final Logger log = LoggerFactory.getLogger(MainSubmitter.class);

    /**
     * Submitter receiving full submissions
     */
    private static volatile SpySubmitter submitter;

    private static volatile Tracer t;

    /**
     * Local tracer (old implementation)
     */
    private static volatile LTracer lt;

    /**
     * Streaming tracer (new implementation)
     */
    private static volatile STracer st;

    /**
     * Thread local
     */
    private static ThreadLocal<Boolean> inSubmit = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    public MainSubmitter() {

    }

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
            t.getHandler().disable();
            if (submitter != null) {
                inSubmit.set(true);
                submitter.submit(stage, id, submitFlags, vals);
            } else if (log.isDebugEnabled()) {
                log.debug("Skipping submit because submitter is not set.");
            }
        } catch (EvalError e) {
            log.debug("Error submitting value from instrumented code: ", e);
            AgentDiagnostics.inc(AgentDiagnostics.SPY_ERRORS);
        } catch (Throwable e) {
            // This is special case. We must catch everything going out of agent, even OOM errors.
            log.debug("Error submitting value from instrumented code: ", e);
            AgentDiagnostics.inc(AgentDiagnostics.SPY_ERRORS);
        } finally {
            inSubmit.set(false);
            t.getHandler().enable();
        }
    }


    /**
     * This method is called by tracer probes at method start.
     *
     * @param classId  class ID (registered)
     * @param methodId method ID (registered)
     */
    public static void traceEnter(int classId, int methodId, int signatureId) {

        if (lt != null) {
            try {
                lt.getLtHandler().traceEnter(classId, methodId, signatureId, System.nanoTime());
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceEnter", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isTraceEnabled()) {
            log.trace("Submitter is null !");
        }

    }

    /**
     * This method is called by tracer probes at method start.
     *
     * @param methodId method ID (registered)
     */
    public static void traceEnterS(int methodId) {
        if (st != null) {
            try {
                st.getStHandler().traceEnter(methodId);
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceEnterS", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isTraceEnabled()) {
            log.trace("Submitter is null !");
        }
    }


    /**
     * This method is called by tracer probes at method exit.
     */
    public static void traceReturn() {

        if (lt != null) {
            try {
                lt.getLtHandler().traceReturn(System.nanoTime());
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceReturn", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isWarnEnabled()) {
            log.warn("Submitter is null !");
        }
    }

    /**
     * This method is called by tracer probes at method exit.
     */
    public static void traceReturnS() {

        if (st != null) {
            try {
                st.getStHandler().traceReturn();
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceReturn", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isTraceEnabled()) {
            log.trace("Submitter is null !");
        }
    }


    /**
     * This method is called by tracer probes at method error point.
     *
     * @param exception exception thrown
     */
    public static void traceError(Throwable exception) {

        if (lt != null) {
            try {
                lt.getLtHandler().traceError(exception, System.nanoTime());
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceError", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isWarnEnabled()) {
            log.warn("Submitter is null !");
        }
    }

    /**
     * This method is called by tracer probes at method error point.
     *
     * @param exception exception thrown
     */
    public static void traceErrorS(Throwable exception) {
        if (st != null) {
            try {
                st.getStHandler().traceError(exception);
            } catch (Throwable e) {
                // This is special case. We must catch everything going out of agent, even OOM errors.
                log.debug("Error executing traceError", e);
                AgentDiagnostics.inc(AgentDiagnostics.TRACER_ERRORS);
            }
        } else if (log.isTraceEnabled()) {
            log.warn("Submitter is null !");
        }
    }

    /**
     * Sets backing spy submitter
     *
     * @param submitter spy submitter
     */
    public synchronized static void setSubmitter(SpySubmitter submitter) {
        MainSubmitter.submitter = submitter;
    }


    /**
     * Sets backing trace event handler.
     *
     * @param tracer
     */
    public synchronized static void setLTracer(LTracer tracer) {
        t = lt = tracer;
    }

    public synchronized static void setSTracer(STracer tracer) {
        t = st = tracer;
    }

    public synchronized static boolean isStreamingTracer() {
        return MainSubmitter.st != null;
    }
}
