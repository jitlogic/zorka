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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.core.spy.lt.LTracer;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;
import com.jitlogic.zorka.core.spy.plugins.*;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.util.OverlayClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public abstract class TracerLib {

    public static final Logger log = LoggerFactory.getLogger(TracerLib.class);

    public static final int SUBMIT_TRACE = TraceMarker.SUBMIT_TRACE;
    public static final int ALL_METHODS = TraceMarker.ALL_METHODS;
    public static final int DROP_INTERIM = TraceMarker.DROP_INTERIM;
    public static final int TRACE_CALLS = TraceMarker.TRACE_CALLS;
    public static final int ERROR_MARK = TraceMarker.ERROR_MARK;
    public static final int DROP_TRACE = TraceMarker.DROP_TRACE;
    public static final int SUBMIT_MEHTOD = TraceMarker.SUBMIT_METHOD;

    public final static int TR_FORCE_TRACE = TraceRecord.FORCE_TRACE;

    public final static String DTRACE_UUID = "DTRACE_UUID";
    public final static String DTRACE_IN   = "DTRACE_IN";
    public final static String DTRACE_OUT  = "DTRACE_OUT";
    public final static String DTRACE_XTT = "DTRACE_XTT";

    public final static String DTRACE_STATE = "DTRACE";

    public final static String DTRACE_SEP  = "_";

    public final static String DTRACE_UUID_HDR = "x-zorka-dtrace-uuid";
    public final static String DTRACE_TID_HDR  = "x-zorka-dtrace-tid";
    public final static String DTRACE_XTT_HDR  = "x-zorka-dtrace-xtt";

    public static final int TUNING_OFF = 0x00;
    public static final int TUNING_SUM = 0x01;
    public static final int TUNING_DET = 0x02;


    protected Tracer tracer;
    protected SymbolRegistry symbolRegistry;
    protected int defaultTraceFlags = TraceMarker.DROP_INTERIM;

    protected final ThreadLocal<DTraceState> dtraceLocal = new ThreadLocal<DTraceState>();

    public TracerLib(Tracer tracer) {
        this.tracer = tracer;
    }

    public void clearOutputs() {
        tracer.shutdown();
    }

    /**
     * Adds matching method to tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void include(String... matchers) {
        for (String matcher : matchers) {
            if (matcher != null) {
                log.debug("Tracer include: " + matcher);
                tracer.include(SpyMatcher.fromString(matcher));
            }
        }
    }

    public void include(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            if (matcher != null) {
                log.debug("Tracer include: " + matcher);
                tracer.include(matcher);
            }
        }
    }

    /**
     * Exclude classes/methods from tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void exclude(String... matchers) {
        for (String matcher : matchers) {
            if (matcher != null) {
                log.debug("Tracer exclude: " + matcher);
                tracer.include(SpyMatcher.fromString(matcher).exclude());
            }
        }
    }

    public void exclude(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            if (matcher != null) {
                log.debug("Tracer exclude: " + matcher);
                tracer.include((matcher).exclude());
            }
        }
    }

    public String listIncludes() {
        StringBuilder sb = new StringBuilder();
        for (SpyMatcher sm : tracer.getMatcherSet().getMatchers()) {
            sb.append(sm.hasFlags(SpyMatcher.EXCLUDE_MATCH) ? "excl: " : "incl: ");
            sb.append(sm);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Starts a new (named) trace.
     *
     * @param name trace name
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name) {
        return begin(name, -1);
    }

    /**
     * Starts new trace.
     *
     * @param name             trace name
     * @param minimumTraceTime minimum trace time
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime) {
        return begin(name, minimumTraceTime, defaultTraceFlags);
    }

    /**
     * Starts new trace.
     *
     * @param name             trace name
     * @param minimumTraceTime minimum trace time
     * @param flags            initial flags
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime, int flags) {
        return new TraceBeginProcessor(tracer, name, minimumTraceTime * 1000000L, flags, symbolRegistry);
    }

    public void traceBegin(String name) {
        traceBegin(name, 0);
    }

    public void traceBegin(String name, long minimumTraceTime) {
        traceBegin(name, minimumTraceTime, defaultTraceFlags);
    }

    public void traceBegin(String name, long minimumTraceTime, int flags) {
        TraceHandler traceBuilder = tracer.getHandler();
        traceBuilder.traceBegin(symbolRegistry.symbolId(name), System.currentTimeMillis(), flags);
        traceBuilder.setMinimumTraceTime(minimumTraceTime);
    }

    public SpyProcessor inTrace(String traceName) {
        return new TraceCheckerProcessor(tracer, symbolRegistry.symbolId(traceName));
    }

    public boolean isInTrace(String traceName) {
        return tracer.getHandler().isInTrace(symbolRegistry.symbolId(traceName));
    }

    /**
     * Creates spy processor that attaches attribute to current trace record.
     *
     * @param attrName destination attribute name (in trace data)
     * @param srcField source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String attrName, String srcField) {
        return attr(attrName, null, srcField);
    }

    /**
     * Creates spy processor that attaches attribute to top record of named trace
     * @param traceName trace name to look up
     * @param attrName attribute name
     * @param srcField source field
     * @return spy processor
     */
    public SpyProcessor traceAttr(String traceName, String attrName, String srcField) {
        return traceAttr(traceName, attrName, null, srcField);
    }


    /**
     * Creates spy processor that formats a string and attaches it as attribute to trace record.
     *
     * @param attrName  destination attribute name (in trace data)
     * @param srcFormat source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor formatAttr(String attrName, String srcFormat) {
        return formatAttr(attrName, null, srcFormat);
    }

    public SpyProcessor formatTraceAttr(String traceName, String attrName, String srcFormat) {
        return formatTraceAttr(traceName, attrName, null, srcFormat);
    }

    /**
     * Creates spy processor that attaches tagged attribute to trace record.
     *
     * @param attrName destination attribute name (in trace data)
     * @param attrTag  attribute tag;
     * @param srcField source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String attrName, String attrTag, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                srcField, attrName, attrTag);
    }

    public SpyProcessor traceAttr(String traceName, String attrName, String attrTag, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                srcField, traceName, attrName, attrTag);
    }


    /**
     * Creates spy processor that formats a string and attaches it as tagged attribute to trace record.
     *
     * @param attrName  destination attribute name (in trace data)
     * @param attrTag   attribute tag;
     * @param srcFormat source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor formatAttr(String attrName, String attrTag, String srcFormat) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
                srcFormat, attrName, attrTag);
    }


    public SpyProcessor formatTraceAttr(String traceName, String attrName, String attrTag, String srcFormat) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
                srcFormat, traceName, attrName, attrTag);
    }


    /**
     * Adds trace attribute to trace record immediately. This is useful for programmatic attribute setting.
     *
     * @param attrName attribute name
     * @param value    attribute value
     */
    public void newAttr(String attrName, Object value) {
        tracer.getHandler().newAttr(-1, symbolRegistry.symbolId(attrName), value);
    }

    /**
     * @param traceName
     * @param attrName
     * @param value
     */
    public void newTraceAttr(String traceName, String attrName, Object value) {
        tracer.getHandler().newAttr(symbolRegistry.symbolId(traceName), symbolRegistry.symbolId(attrName), value);
    }

    /**
     * Adds trace attribute to trace record immediately. This is useful for programmatic attribute setting.
     *
     * @param attrName attribute name
     * @param value    attribute value
     */
    public void newAttr(String attrName, String tag, Object value) {
        tracer.getHandler().newAttr(-1, symbolRegistry.symbolId(attrName), new TaggedValue(symbolRegistry.symbolId(tag), value));
    }

    /**
     * @param traceName
     * @param attrName
     * @param tag
     * @param value
     */
    public void newTraceAttr(String traceName, String attrName, String tag, Object value) {
        tracer.getHandler().newAttr(
                symbolRegistry.symbolId(traceName), symbolRegistry.symbolId(attrName),
                new TaggedValue(symbolRegistry.symbolId(tag), value));
    }

    public SpyProcessor markError() {
        return flags(SUBMIT_TRACE|ERROR_MARK);
    }

    /**
     * Creates spy processor that sets flags in trace marker.
     *
     * @param flags flags to set
     * @return spy processor object
     */
    public SpyProcessor flags(int flags) {
        return new TraceFlagsProcessor(tracer, null, 0, flags);
    }

    public SpyProcessor traceFlags(String traceName, int flags) {
        return new TraceFlagsProcessor(tracer, null, symbolRegistry.symbolId(traceName), flags);
    }

    public SpyProcessor recordFlags(int flags) {
        return new TraceRecordFlagsProcessor(tracer, flags);
    }

    public void newFlags(int flags) {
        tracer.getHandler().markTraceFlags(0, flags);
    }

    /**
     * Creates spy processor that sets flags in trace marker only if given record field is null.
     *
     * @param srcField spy record field to be checked
     * @param flags    flags to set
     * @return spy processor object
     */
    public SpyProcessor flags(String srcField, int flags) {
        return new TraceFlagsProcessor(tracer, srcField, 0, flags);
    }


    public SpyProcessor traceFlags(String srcField, String traceName, int flags) {
        return new TraceFlagsProcessor(tracer, srcField, symbolRegistry.symbolId(traceName), flags);
    }


    public SpyProcessor dtraceInput(long threshold) {
        return new DTraceInputProcessor(this, dtraceLocal, threshold);
    }

    public SpyProcessor dtraceOutput(boolean nextTid, boolean setAttrs) {
        return new DTraceOutputProcessor(this, dtraceLocal, nextTid, setAttrs);
    }

    public SpyProcessor dtraceOutput() {
        return new DTraceOutputProcessor(this, dtraceLocal);
    }

    public SpyProcessor dtraceClean() {
        return new DTraceCleanProcessor(this, dtraceLocal);
    }

    /**
     * Creates trace network sender using HTTP protocol and CBOR representation.
     *
     * @param config Config map
     * @return Submitter object (can be registered later on via tracer.output())
     */
    public abstract ZorkaAsyncThread<SymbolicRecord> toCbor(Map<String, String> config);

    public SpyProcessor filterBy(String srcField, Boolean defval, Set<Object> yes, Set<Object> no, Set<Object> maybe) {
        return new TraceFilterProcessor(tracer, srcField, defval, yes, no, maybe);
    }

    public void filterTrace(boolean decision) {
        tracer.getHandler().markTraceFlags(0, decision ? TraceMarker.SUBMIT_TRACE : TraceMarker.DROP_TRACE);
    }

    public long getTracerMinMethodTime() {
        return TraceHandler.getMinMethodTime();
    }

    /**
     * Sets minimum traced method execution time. Methods that took less time
     * will be discarded from traces and will only reflect in summary call/error counters.
     *
     * @param methodTime minimum execution time (in nanoseconds, 250 microseconds by default)
     */
    public void setTracerMinMethodTime(int methodTime) {
        TraceHandler.setMinMethodTime(methodTime);
    }

    /**
     * Sets minimum traced method execution time. Methods that took less time
     * will be discarded from traces and will only reflect in summary call/error counters.
     *
     * @param methodTime minimum execution time (in nanoseconds, 250 microseconds by default)
     */
    public void setTracerMinMethodTime(long methodTime) {
        TraceHandler.setMinMethodTime(methodTime);
    }

    public long getTracerMinTraceTime() {
        return TraceMarker.getMinTraceTime() / 1000000L;
    }

    /**
     * Sets minimum trace execution time. Traces that laster for shorted period
     * of time will be discarded. Not that this is default setting that can be
     * overridden with spy.begin() method.
     *
     * @param traceTime minimum trace execution time (50 milliseconds by default)
     */
    public void setTracerMinTraceTime(int traceTime) {
        TraceMarker.setMinTraceTime(traceTime * 1000000L);
    }

    /**
     * Sets minimum trace execution time. Traces that laster for shorted period
     * of time will be discarded. Not that this is default setting that can be
     * overridden with spy.begin() method.
     *
     * @param traceTime minimum trace execution time (50 milliseconds by default)
     */
    public void setTracerMinTraceTime(long traceTime) {
        TraceMarker.setMinTraceTime(traceTime * 1000000L);
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
        TraceHandler.setMaxTraceRecords(maxRecords);
    }

    public void setTracerMaxTraceRecords(long maxRecords) {
        TraceHandler.setMaxTraceRecords((int) maxRecords);
    }

    public int getTracerMaxTraceRecords() {
        return TraceHandler.getMaxTraceRecords();
    }

    public void setTracerMinTraceCalls(int minCalls) {
        TraceHandler.setMinTraceCalls(minCalls);
    }

    public void setTracerMinTraceCalls(long minCalls) {
        TraceHandler.setMinTraceCalls((int)minCalls);
    }

    public int getTracerMinTraceCalls() {
        return TraceHandler.getMinTraceCalls();
    }

    public void setTraceSpyMethods(boolean tsm) {
        tracer.setTraceSpyMethods(tsm);
    }

    public boolean isTraceSpyMethods() {
        return tracer.isTraceSpyMethods();
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

    public ClassLoader overlayClassLoader(ClassLoader parent, String pattern, ClassLoader overlay) {
        return new OverlayClassLoader(parent, pattern, overlay);
    }

    /**
     * Configures tracer output.
     *
     * @param output trace processing object
     */
    public void output(ZorkaSubmitter<SymbolicRecord> output) {
        tracer.addOutput(output);
    }

    public String tunerStatus() {
        TracerTuner tt = tracer.getTracerTuner();
        if (tt == null) return "N/A";

        return tt.getStatus();
    }

    public String tunerExclude(int nitems) {
        TracerTuner tt = tracer.getTracerTuner();
        if (tt == null) return "Tuner is disabled.";

        return "Excluded: " +  tt.exclude(nitems, true) + " (items left: " + tt.getRankList().size() + ")";
    }

    public void setTuningEnabled(boolean tuningEnabled) {
        TraceHandler.setTuningEnabled(tuningEnabled);
    }
}
