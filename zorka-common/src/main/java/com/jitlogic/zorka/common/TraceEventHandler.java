/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common;

public abstract class TraceEventHandler {

    /**
     * Records beginning of a trace. Not that sometimes traces can be recursive.
     *
     * @param traceId trace ID (symbol)
     */
    public abstract void traceBegin(int traceId, long clock, int flags);

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
    public abstract void traceEnter(int classId, int methodId, int signatureId, long tstamp);

    /**
     * Records method return.
     *
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     */
    public abstract void traceReturn(long tstamp);

    /**
     * Records method error (exception thrown from method).
     *
     * @param exception exception object (wrapped or unserialized)
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     *
     */
    public abstract void traceError(Object exception, long tstamp);

    /**
     * Records a parameter.
     *
     * @param attrId parametr ID
     *
     * @param attrVal parameter value
     */
    public abstract void newAttr(int attrId, Object attrVal);
}
