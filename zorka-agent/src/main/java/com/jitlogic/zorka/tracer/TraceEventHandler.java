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

package com.jitlogic.zorka.tracer;

/**
 * Handles trace events. For performance reasons this is class, not interface
 * (and null implementation is useful anyway).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceEventHandler {

    /**
     * Records beginning of a trace. Not that sometimes traces can be recursive.
     *
     * @param traceId trace ID (symbol)
     */
    public void traceBegin(int traceId) {

    }


    /**
     * Records metod entry.
     *
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     *
     * @param classId class ID (class name symbol ID)
     *
     * @param methodId method ID (method name symbol ID)
     *
     * @param signatureId signature ID (method signature symbol ID)
     */
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {

    }


    /**
     * Records method return.
     *
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     */
    public void traceReturn(long tstamp) {

    }


    /**
     * Records method error (exception thrown from method).
     *
     * @param exception exception object (wrapped or unserialized)
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     *
     */
    public void traceError(TracedException exception, long tstamp) {

    }


    /**
     * Records trace statistics.
     *
     * @param calls number of (recursive, traced) calls
     *
     * @param errors number of errors
     */
    public void traceStats(long calls, long errors) {

    }


    /**
     * Records symbol (to be later used as class/method/parameter ID).
     * Note that symbols are mostly generated at config / class loading time
     * and are not normally emitted from instrumented code.
     *
     * @param symbolId numeric symbol ID
     *
     * @param symbolText symbol text
     */
    public void newSymbol(int symbolId, String symbolText) {

    }


    /**
     * Records a parameter.
     *
     * @param attrId parametr ID
     *
     * @param attrVal parameter value
     */
    public void newAttr(int attrId, Object attrVal) {

    }

}
