/** 
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent;

import bsh.EvalError;

import java.io.Closeable;
import java.io.IOException;

/**
 * Represents runnable task performing BSH query.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ZorkaBshWorker implements Runnable, Closeable {

    // TODO integrate with ZorkaCallback, create single interface representing zorka query execution task;

    /** Reference to Zorka BSH agent */
	private final ZorkaBshAgent agent;

    /** Expression (query) to be performed */
	private final String expr;

    /** Callback object (to report query result) */
	private final ZorkaCallback callback;

    /**
     * Creates new BSH worker object.
     *
     * @param agent reference to BSH agent
     *
     * @param expr BSH expression
     *
     * @param callback callback used to report query result
     */
	public ZorkaBshWorker(ZorkaBshAgent agent, String expr, ZorkaCallback callback) {
		this.agent = agent;
		this.expr = expr;
		this.callback = callback;
	}


    @Override
	public void run() {
		try {
			callback.handleResult(agent.eval(expr));
		} catch (EvalError e) {
            callback.handleError(e);
        } catch (Exception e) {
            callback.handleError(e);
        }
	}


    @Override
    public void close() throws IOException {
        callback.handleError(new RuntimeException("Request timed out."));
    }
}
