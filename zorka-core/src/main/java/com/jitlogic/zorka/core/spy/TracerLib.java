/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.output.DTraceFormatter;
import com.jitlogic.zorka.core.spy.output.DTraceFormatterZJ;
import com.jitlogic.zorka.core.spy.output.DTraceOutput;
import com.jitlogic.zorka.core.spy.plugins.*;
import com.jitlogic.zorka.core.spy.tuner.TracerTuner;
import com.jitlogic.zorka.core.util.OverlayClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class TracerLib {

    private static final Logger log = LoggerFactory.getLogger(TracerLib.class);

    public static final int SUBMIT_TRACE = TraceMarker.SUBMIT_TRACE;
    public static final int ALL_METHODS = TraceMarker.ALL_METHODS;
    public static final int DROP_INTERIM = TraceMarker.DROP_INTERIM;
    public static final int TRACE_CALLS = TraceMarker.TRACE_CALLS;
    public static final int ERROR_MARK = TraceMarker.ERROR_MARK;
    public static final int DROP_TRACE = TraceMarker.DROP_TRACE;
    public static final int SUBMIT_MEHTOD = TraceMarker.SUBMIT_METHOD;

    public final static int TR_FORCE_TRACE = TraceRecord.FORCE_TRACE;

    public static final int TUNING_OFF = 0x00;
    public static final int TUNING_SUM = 0x01;
    public static final int TUNING_DET = 0x02;

    // Attribute processing flags
    public static final int PA_MAP_OF_ANY     = 0x0001;
    public static final int PA_MAP_OF_LISTS_1 = 0x0002;
    public static final int PA_MAP_OF_LISTS_N = 0x0003;
    public static final int PA_MAP            = 0x000f;

    public static final int PA_LIST_OF_ANY    = 0x0010;
    public static final int PA_LIST           = 0x00f0;

    public static final int PA_TO_STRING      = 0x1000;
    public static final int PA_WARN_ON_NULL   = 0x2000;

    public final static String DTRACE_STATE = "DTRACE";

    public final static String DH_B3_TRACEID = "x-b3-traceid";
    public final static String DH_B3_SPANID = "x-b3-spanid";
    public final static String DH_B3_PARENTID = "x-b3-parentspanid";
    public final static String DH_B3_SAMPLED = "x-b3-sampled";
    public final static String DH_B3_FLAGS = "x-b3-flags";
    public final static String DH_B3 = "b3";

    public final static String DH_UBER_TID = "uber-trace-id";
    public final static String DH_UBER_CTX = "uberctx-";

    public final static String DH_W3_TRACEPARENT = "traceparent";
    public final static String DH_W3_TRACESTATE = "tracestate";

    public final static Set<String> CTX_HEADERS = ZorkaUtil.constSet(DH_B3_TRACEID, DH_B3_SPANID, DH_B3_PARENTID,
            DH_B3_FLAGS, DH_B3_SAMPLED, DH_B3, DH_UBER_TID, DH_W3_TRACEPARENT, DH_W3_TRACESTATE);

    public final static String DT_TRACE_ID = "TRACE_ID";
    public final static String DT_SPAN_ID  = "SPAN_ID";
    public final static String DT_PARENT_ID = "PARENT_ID";


    public static final int F_SAMPLE      = 0x000001; // SAMPLE flag
    public static final int F_DEBUG       = 0x000100; // DEBUG flag
    public static final int F_DROP        = 0x000200; // DROP trace flag
    public static final int F_B3_HDR      = 0x000400; // Use short B3 header form
    public static final int F_SENT        = 0x000800; // Span already sent

    public static final int DFM_MASK   = 0x0f0000; // Context propagation mode
    public static final int DFM_ZIPKIN = 0x010000; // Zipkin context propagation, x-b3-* headers
    public static final int DJM_JAEGER = 0x020000; // Jaeger context propagation
    public static final int DFM_W3C    = 0x030000; // W3C Context Propagation

    /** Span kinds */
    public static final int DFK_MASK       = 0xf00000; // Kind mask
    public static final int DFK_CLIENT     = 0x100000; // Kind: client request
    public static final int DFK_SERVER     = 0x200000; // Kind: server request handling
    public static final int DFK_PRODUCER   = 0x300000; // Kind: message send
    public static final int DFK_CONSUMER   = 0x400000; // Kind: message receive (& handling)

    protected Tracer tracer;
    protected ZorkaConfig config;
    protected SymbolRegistry symbolRegistry;
    protected MetricsRegistry metricsRegistry;
    protected int defaultTraceFlags = TraceMarker.DROP_INTERIM;

    public static final Map<String,String> OPENTRACING_TAGS = ZorkaUtil.constMap(
            "REMOTE_IP", "peer.ipv4",
            "REMOTE_IP6", "peer.ipv6",
            "REMOTE_PORT", "peer.port",
            "REMOTE_SVC", "peer.service",
            "LOCAL_IP", "local.ipv4",
            "LOCAL_IP6", "local.ipv6",
            "LOCAL_PORT", "local.port",
            "LOCAL_SVC", "local.service",
            "SQL", "db.statement",
            "DB", "db.instance",
            // TODO db.instance - database name
            // TODO db.type - 'sql', 'redis', 'cassandra', 'hbase', 'redis'
            // TODO db.user
            "METHOD", "http.method",
            "STATUS", "http.status_code",
            "URL", "http.url",
            "URI", "http.uri"
            // TODO peer.address
            // TODO peer.hostname
            // TODO sampling.priority - 0 or more
            // TODO span.kind - client, server, producer, consumer
            // TODO message_bus.destination
    );

    public TracerLib(SymbolRegistry symbolRegistry, MetricsRegistry metricsRegistry, Tracer tracer, ZorkaConfig config) {
        this.symbolRegistry = symbolRegistry;
        this.metricsRegistry = metricsRegistry;
        this.tracer = tracer;
        this.config = config;

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
                log.info("Tracer include: " + matcher);
                tracer.include(SpyMatcher.fromString(matcher));
            }
        }
    }

    public void include(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            if (matcher != null) {
                log.info("Tracer include: " + matcher);
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
                log.info("Tracer exclude: " + matcher);
                tracer.include(SpyMatcher.fromString(matcher).exclude());
            }
        }
    }

    public void exclude(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            if (matcher != null) {
                log.info("Tracer exclude: " + matcher);
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
     * Creates spy processor that attaches attribute to top record of named trace
     * @param traceName trace name to look up
     * @param attrName attribute name
     * @param srcField source field
     * @return spy processor
     */
    public SpyProcessor traceAttr(String traceName, String attrName, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                traceName, ZorkaUtil.<String, String>constMap(attrName, srcField));
    }


    /**
     * Creates spy processor that formats a string and attaches it as attribute to trace record.
     *
     * @param attrName  destination attribute name (in trace data)
     * @param srcFormat source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor formatAttr(String attrName, String srcFormat) {
        return formatTraceAttr(null, attrName, srcFormat);
    }

    public SpyProcessor formatAttrs(Map<String,String> cattrs, String...eattrs) {
        return formatTraceAttrs(null, cattrs, eattrs);
    }

    public SpyProcessor formatTraceAttrs(String traceName, Map<String,String> cattrs, String...eattrs) {
        Map<String,String> attrs = cattrs != null ? new TreeMap<String,String>(cattrs) : new TreeMap<String,String>();
        for (int i = 1; i < eattrs.length; i+=2) {
            attrs.put(eattrs[i-1], eattrs[i]);
        }
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
            traceName, attrs);
    }

    public SpyProcessor formatTraceAttr(String traceName, String attrName, String srcFormat) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
                traceName, ZorkaUtil.<String, String>constMap(attrName, srcFormat));
    }

    /**
     * Creates spy processor that attaches tagged attribute to trace record.
     *
     * @param attrName destination attribute name (in trace data)
     * @param srcField source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String attrName, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                null, ZorkaUtil.<String, String>constMap(attrName, srcField));
    }


    /**
     * Helper function for processing incoming (outgoing) headers as trace attributes.
     * @param rec context record
     * @param name header name
     * @param index index (if many headers with the same name)
     * @param value header value
     * @param prefix prefix to add
     */
    public void procHeader(Map<String,Object> rec, String name, int index, String value, String prefix) {
        if (name == null) return;

        if (index == 0) {
            newAttr(prefix + name + "." + index, value);
        }

        name = name.toLowerCase();

        if (rec != null && (CTX_HEADERS.contains(name) || name.startsWith(DH_UBER_CTX))) {
            rec.put(name, value);
        }
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

    public SpyProcessor procAttr(int flags, String prefix, String src, String...path) {
        return new ProcAttrProcessor(this, flags, prefix, src, path);
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

    public SpyProcessor dtraceInput(int defFlags, int addFlags) {
        return new DTraceInputProcessor(tracer, this, defFlags, addFlags);
    }

    public SpyProcessor dtraceOutput(int delFlags, int addFlags) {
        return new DTraceOutputProcessor(tracer, this, delFlags, addFlags);
    }

    public DTraceFormatter zipkinJsonFormatter(Map<String,String> tagMap) {
        return new DTraceFormatterZJ(config, symbolRegistry, tagMap);
    }

    public ZorkaSubmitter<SymbolicRecord> toDtraceOutput(DTraceFormatter formatter, ZorkaSubmitter<byte[]> sender) {
        return new DTraceOutput(formatter, sender);
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
        log.info("Adding output: " + output);
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
