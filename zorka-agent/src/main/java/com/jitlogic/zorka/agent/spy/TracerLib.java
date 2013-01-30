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
import com.jitlogic.zorka.agent.rankproc.JmxAttrScanner;
import com.jitlogic.zorka.agent.rankproc.QueryLister;
import com.jitlogic.zorka.common.Submittable;
import com.jitlogic.zorka.common.SymbolRegistry;
import com.jitlogic.zorka.common.TraceEventHandler;
import com.jitlogic.zorka.common.ZorkaAsyncThread;

public class TracerLib {

    private SpyInstance instance;
    private Tracer tracer;
    private SymbolRegistry symbols;


    public TracerLib(SpyInstance instance) {
        this.instance = instance;
        this.tracer = this.instance.getTracer();
        symbols = this.tracer.getSymbolRegistry();
    }


    /**
     * Configures tracer output.
     *
     * @param output trace processing object
     */
    public void tracerOutput(ZorkaAsyncThread<Submittable> output) {
        instance.getTracer().setOutput(output);
    }


    /**
     * Adds matching method to tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void traceInclude(SpyMatcher...matchers) {
        for (SpyMatcher matcher : matchers) {
            instance.getTracer().include(matcher);
        }
    }

    /**
     * Exclude classes/methods from tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void traceExclude(SpyMatcher...matchers) {
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
    public SpyProcessor traceBegin(String name) {
        return traceBegin(name, -1);
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
    public SpyProcessor traceBegin(String name, long minimumTraceTime) {
        return traceBegin(name, minimumTraceTime, TraceMarker.DROP_INTERIM);
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
    public SpyProcessor traceBegin(String name, long minimumTraceTime, int flags) {
        return new TraceBeginProcessor(instance.getTracer(), name, minimumTraceTime * 1000000L, flags);
    }


    /**
     * Attaches attribute to trace record.
     *
     * @param srcField source field name (from spy record)
     * @param dstAttr destination attribute name (in trace data)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor traceAttr(String srcField, String dstAttr) {
        return new TraceAttrProcessor(instance.getTracer(), srcField, dstAttr);
    }


    public SpyProcessor traceFlags(int flags) {
        return new TraceFlagsProcessor(instance.getTracer(), null, flags);
    }


    public SpyProcessor traceFLags(String srcField, int flags) {
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
    public ZorkaAsyncThread<Submittable> traceFile(String path, int maxFiles, long maxSize) {
        TraceFileWriter writer = new TraceFileWriter(ZorkaConfig.propFormat(path),
                instance.getTracer().getSymbolRegistry(), maxFiles, maxSize);
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
     * overridden with spy.traceBegin() method.
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


    public JmxAttrScanner jmxScanner(String name, TraceEventHandler output, QueryLister...listers) {
        return new JmxAttrScanner(symbols, name, output, listers);
    }

}
