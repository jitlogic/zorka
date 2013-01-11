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

package com.jitlogic.zorka.spy;

/**
 * Handles trace events. This is
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public interface TraceEventHandler {

    public static final byte NEW_TRACE = 0x00;
    public static final byte ENTER_METHOD  = 0x01;
    public static final byte RETURN_METHOD = 0x02;
    public static final byte ERROR_METHOD  = 0x03;
    public static final byte SYMBOL = 0x04;
    public static final byte S_PARAM = 0x05;
    public static final byte I_PARAM = 0x06;
    public static final byte L_PARAM = 0x07;
    public static final byte D_PARAM = 0x08;


    /**
     * Records beginning of new trace.
     *
     * @param traceId trace ID
     *
     * @param tstamp timestamp
     */
    void newTrace(int traceId, long tstamp);


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
    void traceEnter(int classId, int methodId, int signatureId, long tstamp);


    /**
     * Records method return.
     *
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     */
    void traceReturn(long tstamp);


    /**
     * Records method error (exception thrown from method).
     *
     * @param tstamp timestamp (in nanoseconds since Epoch - see System.nanoTime())
     *
     * @param exception exception object
     */
    void traceError(Throwable exception, long tstamp);


    /**
     * Records symbol (to be later used as class/method/parameter ID).
     * Note that symbols are mostly generated at config / class loading time
     * and are not normally emitted from instrumented code.
     *
     * @param symbolId numeric symbol ID
     *
     * @param symbolText symbol text
     */
    void newSymbol(int symbolId, String symbolText);


    /**
     * Records string parameter
     *
     * @param parId parametr ID
     *
     * @param val parameter value
     */
    void newParam(int parId, String val);


    /**
     * Records integer parameter
     *
     * @param parId parameter ID
     *
     * @param val parameter value
     */
    void newParam(int parId, int val);


    /**
     * Records long integer parameter
     *
     * @param parId parameter ID
     *
     * @param val parameter value
     */
    void newParam(int parId, long val);


    /**
     * Records double number parameter
     *
     * @param parId parameter ID
     *
     * @param val parameter value
     */
    void newParam(int parId, double val);

}
