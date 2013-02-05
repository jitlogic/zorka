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

package com.jitlogic.zorka.agent.spy;

import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.common.*;

/**
 * Tracer library contains functions for configuring and using tracer.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TracerLib {

    public static final int ALWAYS_SUBMIT = TraceMarker.ALWAYS_SUBMIT;

    public static final int ALL_METHODS = TraceMarker.ALL_METHODS;

    public static final int DROP_INTERIM = TraceMarker.DROP_INTERIM;


    /** Reference to spy instance */
    private SpyInstance instance;

    /** Default trace flags */
    private int defaultTraceFlags = TraceMarker.DROP_INTERIM;

    /**
     * Creates tracer library object.
     *
     * @param instance reference to spy instance
     */
    public TracerLib(SpyInstance instance) {
        this.instance = instance;
    }


    /**
     * Configures tracer output.
     *
     * @param output trace processing object
     */
    public void output(ZorkaAsyncThread<Submittable> output) {
        instance.getTracer().addOutput(output);
    }


    /**
     * Adds matching method to tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void include(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            instance.getTracer().include(matcher);
        }
    }

    /**
     * Exclude classes/methods from tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void exclude(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            instance.getTracer().include(matcher.exclude());
        }
    }

    /**
     * Starts a new (named) trace.
     *
     * @param name trace name
     *
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name) {
        return begin(name, -1);
    }


    /**
     * Starts new trace.
     *
     * @param name trace name
     *
     * @param minimumTraceTime minimum trace time
     *
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime) {
        return begin(name, minimumTraceTime, defaultTraceFlags);
    }


    /**
     * Starts new trace.
     *
     * @param name trace name
     *
     * @param minimumTraceTime minimum trace time
     *
     * @param flags initial flags
     *
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime, int flags) {
        return new TraceBeginProcessor(instance.getTracer(), name, minimumTraceTime * 1000000L, flags);
    }


    /**
     * Creates spy processor that attaches attribute to trace record.
     *
     * @param srcField source field name (from spy record)
     *
     * @param dstAttr destination attribute name (in trace data)
     *
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String srcField, String dstAttr) {
        return new TraceAttrProcessor(instance.getTracer(), srcField, dstAttr);
    }


    /**
     * Creates spy processor that sets flags in trace marker.
     *
     * @param flags flags to set
     *
     * @return spy processor object
     */
    public SpyProcessor flags(int flags) {
        return new TraceFlagsProcessor(instance.getTracer(), null, flags);
    }


    /**
     * Creates spy processor that sets flags in trace marker only if given record field is null.
     *
     * @param srcField spy record field to be checked
     *
     * @param flags flags to set
     *
     * @return spy processor object
     */
    public SpyProcessor flags(String srcField, int flags) {
        return new TraceFlagsProcessor(instance.getTracer(), srcField, flags);
    }


    /**
     * Creates trace file writer object. Trace writer can receive traces and store them in a file.
     *
     * @param path path to a file
     *
     * @param maxFiles maximum number of archived files
     *
     * @param maxSize maximum file size
     *
     * @return trace file writer
     */
    public ZorkaAsyncThread<Submittable> toFile(String path, int maxFiles, long maxSize) {
        TraceFileWriter writer = new TraceFileWriter(ZorkaConfig.formatCfg(path),
                instance.getTracer().getSymbolRegistry(), instance.getTracer().getMetricsRegistry(),
                maxFiles, maxSize);
        writer.start();
        return writer;
    }


    /**
     * Sets minimum traced method execution time. Methods that took less time
     * will be discarded from traces and will only reflect in summary call/error counters.
     *
     * @param methodTime minimum execution time (in nanoseconds, 250 microseconds by default)
     */
    public void setTracerMinMethodTime(long methodTime) {
        Tracer.setMinMethodTime(methodTime);
    }


    /**
     * Sets minimum trace execution time. Traces that laster for shorted period
     * of time will be discarded. Not that this is default setting that can be
     * overridden with spy.begin() method.
     *
     * @param traceTime minimum trace execution time (50 milliseconds by default)
     */
    public void setTracerMinTraceTime(long traceTime) {
        Tracer.setMinTraceTime(traceTime * 1000000L);
    }


    /**
     * Sets maximum number of records that will be stored in a single trace.
     * This setting prevents agent from overrunning memory when instrumented
     * code has very long (and complex) execution path. After maximum number
     * is reached, all remaining records will be discarded but numbers of calls
     * and errors of discarded methods will be reflected in summary data.
     *
     * @param maxRecords maximum numbner of trace records
     */
    public void setTracerMaxTraceRecords(int maxRecords) {
        Tracer.setMaxTraceRecords(maxRecords);
    }


    /**
     * Sets default trace marker flags. This setting will be used when beginning new traces
     * without supplying initial flags.
     *
     * @param flags trace flags
     */
    public void setDefaultTraceFlags(int flags) {
        this.defaultTraceFlags = flags;
    }


}
