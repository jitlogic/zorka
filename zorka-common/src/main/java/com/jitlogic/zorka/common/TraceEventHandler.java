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


/**
 * Handles trace events. For performance reasons this is class, not interface
 * (and null implementation is useful anyway).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public abstract class TraceEventHandler extends TraceBuilder {

    /**
     * Records trace statistics.
     *
     * @param calls number of (recursive, traced) calls
     *
     * @param errors number of errors
     */
    public abstract  void traceStats(long calls, long errors, int flags);


    /**
     * Records symbol (to be later used as class/method/parameter ID).
     * Note that symbols are mostly generated at config / class loading time
     * and are not normally emitted from instrumented code.
     *
     * @param symbolId numeric symbol ID
     *
     * @param symbolText symbol text
     */
    public abstract void newSymbol(int symbolId, String symbolText);


    /**
     * Records a collection of long integer samples.
     *
     * @param clock wall clock time
     *
     * @param objId object ID
     *
     * @param components symbol IDs
     *
     * @param values values
     */
    public abstract void longVals(long clock, int objId, int[] components, long[] values);


    /**
     * Records a collection of floating point samples.
     *
     * @param clock wall clock time
     *
     * @param objId object ID
     *
     * @param components symbol IDs
     *
     * @param values values
     */
    public abstract void doubleVals(long clock, int objId, int[] components, double[] values);


    /**
     * Records newly defined metrics template
     *
     * @param template
     */
    public abstract void newMetricTemplate(MetricTemplate template);

    public abstract void newMetric(Metric metric);
}
