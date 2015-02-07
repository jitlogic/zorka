/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core;

import bsh.EvalError;
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * Represents runnable task performing BSH query.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaBshWorker implements Runnable, Closeable {

    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaBshWorker.class);

    /**
     * Reference to Zorka BSH agent
     */
    private final ZorkaBshAgent agent;

    /**
     * Expression (query) to be performed
     */
    private final String expr;

    /**
     * Callback object (to report query result)
     */
    private final ZorkaCallback callback;

    /**
     * Executor to run real work.
     */
    private ExecutorService executor;

    /**
     * Request handling timeout.
     */
    private long timeout;

    /**
     * Creates new BSH worker object.
     *
     * @param agent    reference to BSH agent
     * @param expr     BSH expression
     * @param callback callback used to report query result
     */
    public ZorkaBshWorker(ExecutorService executor, long timeout, ZorkaBshAgent agent, String expr, ZorkaCallback callback) {
        this.executor = executor;
        this.timeout = timeout;
        this.agent = agent;
        this.expr = expr;
        this.callback = callback;
    }


    @Override
    public void run() {

        long t1 = System.nanoTime();

        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return agent.eval(expr);
            }
        };

        Future<Object> future = executor.submit(task);

        try {
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_REQUESTS);
            callback.handleResult(future.get(timeout, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_ERRORS);
            log.error(ZorkaLogger.ZAG_ERRORS, "Executing expression '" + expr + "' has been interrupted.");
            callback.handleError(new RuntimeException("Request has been interrupted."));
            future.cancel(true);
        } catch (ExecutionException e) {
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_ERRORS);
            log.error(ZorkaLogger.ZAG_ERRORS, "Error evaluating expression '" + expr + "'", e);
            callback.handleError(e.getCause());
        } catch (TimeoutException e) {
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_ERRORS);
            log.error(ZorkaLogger.ZAG_ERRORS, "Timeout executing expression '" + expr + "'.");
            callback.handleError(e);
            future.cancel(true);
        }

        long t2 = System.nanoTime();
        AgentDiagnostics.inc(AgentDiagnostics.AGENT_TIME, t2 - t1);
    }

    //@Override
    public void runOld() {
        long t1 = System.nanoTime();

        try {
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_REQUESTS);
            callback.handleResult(agent.eval(expr));
        } catch (EvalError e) {
            callback.handleError(e);
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_ERRORS);
            log.error(ZorkaLogger.ZAG_ERRORS, "Error evaluating expression '" + expr + "'", e);
        } catch (Exception e) {
            callback.handleError(e);
            AgentDiagnostics.inc(AgentDiagnostics.AGENT_ERRORS);
            log.error(ZorkaLogger.ZAG_ERRORS, "Error evaluating expression '" + expr + "'", e);
        }

        long t2 = System.nanoTime();
        AgentDiagnostics.inc(AgentDiagnostics.AGENT_TIME, t2 - t1);
    }


    @Override
    public void close() throws IOException {
        callback.handleError(new RuntimeException("Request timed out."));
    }
}
