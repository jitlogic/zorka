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

    private static final boolean logTrace = "yes".equalsIgnoreCase(System.getProperty("zorka.tracer.full.trace", "no"));

    /**
     * Submitter receiving full submissions
     */
    private static volatile SpySubmitter submitter;

    private static volatile Tracer t;


    /**
     * Thread local
     */
    private static ThreadLocal<Boolean> inSubmit = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    private MainSubmitter() {
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

        if (logTrace) {
            if (log.isTraceEnabled()) {
                log.trace("Submit: id=" + id + ", stage=" + stage);
            }
        }

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
     * @param mid method def id
     */
    public static void traceEnter(int mid) {

        if (logTrace) {
            if (log.isTraceEnabled()) {
                log.trace("TraceEnter: id=" + mid);
            }
        }

        if (t != null) {
            try {
                t.getHandler().traceEnter(mid, System.nanoTime());
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
     * This method is called by tracer probes at method exit.
     */
    public static void traceReturn() {

        if (logTrace) {
            if (log.isTraceEnabled()) {
                log.trace("TraceReturn:");
            }
        }

        if (t != null) {
            try {
                t.getHandler().traceReturn(System.nanoTime());
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
     * This method is called by tracer probes at method error point.
     *
     * @param exception exception thrown
     */
    public static void traceError(Throwable exception) {

        if (logTrace) {
            if (log.isTraceEnabled()) {
                log.trace("TraceError: " + exception.getMessage());
            }
        }

        if (t != null) {
            try {
                t.getHandler().traceError(exception, System.nanoTime());
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
     * Sets backing spy submitter
     *
     * @param submitter spy submitter
     */
    public synchronized static void setSubmitter(SpySubmitter submitter) {
        MainSubmitter.submitter = submitter;
    }

    public synchronized static void setTracer(Tracer tracer) {
        t = tracer;
    }

    public synchronized static boolean isStreamingTracer() {
        return t != null && t.getClass() == STracer.class;
    }
}
