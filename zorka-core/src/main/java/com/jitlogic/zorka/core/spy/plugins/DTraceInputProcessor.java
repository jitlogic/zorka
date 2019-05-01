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
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.common.tracedata.DTraceContext;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.TracerLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

/**
 *
 */
public class DTraceInputProcessor implements SpyProcessor {

    private static final Logger log = LoggerFactory.getLogger(DTraceInputProcessor.class);

    private Tracer tracer;
    private TracerLib tracerLib;

    private Random rand;

    /** Default flags */
    private int defFlags;

    private int addFlags;

    public final static Pattern RE_DIGIT  = Pattern.compile("\\d+");
    public final static Pattern RE_HEX64  = Pattern.compile("([0-9a-zA-Z]{16})");
    public final static Pattern RE_HEX128 = Pattern.compile("([0-9a-zA-Z]{16})([0-9a-zA-Z]{16})");
    public final static Pattern RE_B3_64 = Pattern.compile("([0-9a-zA-Z]{16})-([0-9a-zA-Z]{16})-(?:([01d])(?:-([0-9a-zA-Z]{16}))?)?");
    public final static Pattern RE_B3_128 = Pattern.compile("([0-9a-zA-Z]{32})-([0-9a-zA-Z]{16})-(?:([01d])(?:-([0-9a-zA-Z]{16}))?)?");
    public final static Pattern RE_JG_64 = Pattern.compile("([0-9a-zA-Z]{16}):([0-9a-zA-Z]{16}):([0-9a-zA-Z]{16}):([0-9a-zA-Z]{2})");
    public final static Pattern RE_JG_128 = Pattern.compile("([0-9a-zA-Z]{32}):([0-9a-zA-Z]{16}):([0-9a-zA-Z]{16}):([0-9a-zA-Z]{2})");
    public final static Pattern RE_W3 = Pattern.compile("([0-9a-zA-Z]{2})-([0-9a-zA-Z]{32})-([0-9a-zA-Z]{16})-([0-9a-zA-Z]{2})");

    public DTraceInputProcessor(Tracer tracer, TracerLib tracerLib, int defFlags, int addFlags) {
        this.tracer = tracer;
        this.tracerLib = tracerLib;
        this.defFlags = defFlags;
        this.addFlags = addFlags;
        this.rand = new Random();
    }

    public static DTraceContext dtrace(String tidStr, String sidStr, String pidStr, int flags) {
        //
        long traceId1 = 0, traceId2 = 0;
        if (tidStr != null) {
            Matcher tidMatch = RE_HEX128.matcher(tidStr);
            if (tidMatch.matches()) {
                traceId1 = new BigInteger(tidMatch.group(1), 16).longValue(); // TODO do not use BigInteger here
                traceId2 = new BigInteger(tidMatch.group(2), 16).longValue();
            } else {
                tidMatch = RE_HEX64.matcher(tidStr);
                if (tidMatch.matches()) {
                    traceId1 = new BigInteger(tidMatch.group(1), 16).longValue();
                }
            }
        }

        long spanId = 0;
        if (sidStr != null) {
            Matcher sidMatch = RE_HEX64.matcher(sidStr);
            if (sidMatch.matches()) {
                spanId = new BigInteger(sidMatch.group(1), 16).longValue();
            }
        }

        long parentId = 0;
        if (pidStr != null) {
            Matcher pidMatch = RE_HEX64.matcher(pidStr);
            if (pidMatch.matches()) {
                parentId = new BigInteger(pidMatch.group(1), 16).longValue();
            }
        }

        if (traceId1 == 0) {
            log.warn("Incomplete trace context headers (zipkin, multiheader)");
            return null;
        }

        return new DTraceContext(traceId1, traceId2, parentId, spanId, System.currentTimeMillis(), flags);
    }

    public static DTraceContext dropTrace() {
        return new DTraceContext(0, 0, 0, 0, 0, F_DROP);
    }

    /** Parses standard zipkin context (bunch of headers). */
    public static DTraceContext parseZipkinCtx(Map<String,Object> rec) {
        String tidStr = (String)rec.get(DH_B3_TRACEID);
        String sidStr = (String)rec.get(DH_B3_SPANID);
        String pidStr = (String)rec.get(DH_B3_PARENTID);

        int flags = DFM_ZIPKIN;
        String smpStr = (String)rec.get(DH_B3_SAMPLED);
        if ("true".equalsIgnoreCase(smpStr) || "1".equalsIgnoreCase(smpStr)) flags |= F_SAMPLE;
        if ("false".equalsIgnoreCase(smpStr) || "0".equalsIgnoreCase(smpStr)) flags |= F_DROP;
        String flgStr = (String)rec.get(DH_B3_FLAGS);
        if ("true".equalsIgnoreCase(flgStr) || "1".equalsIgnoreCase(flgStr)) flags |= F_SAMPLE|F_DEBUG;

        return dtrace(tidStr, sidStr, pidStr, flags);
    }

    public DTraceContext parseZipkinB3Ctx(Map<String,Object> rec) {
        String b3Str = (String)rec.get(DH_B3);

        if (b3Str != null) {
            Matcher m = RE_B3_128.matcher(b3Str);
            if (!m.matches()) m = RE_B3_64.matcher(b3Str);

            if (m.matches()) {
                int flags = DFM_ZIPKIN |F_B3_HDR;
                if ("1".equals(m.group(3))) flags |= F_SAMPLE;
                if ("0".equals(m.group(3))) flags |= F_DROP;
                if ("d".equals(m.group(3))) flags |= F_SAMPLE|F_DEBUG;
                return dtrace(m.group(1), m.group(2), m.group(4), flags);
            } else if ("0".equals(b3Str)) {
                return dropTrace();
            }
        }
        return null;
    }


    private DTraceContext parseJaegerCtx(Map<String,Object> rec) {
        String uberStr = (String)rec.get(DH_UBER_TID);

        if (uberStr != null) {
            Matcher m = RE_JG_128.matcher(uberStr);
            if (!m.matches()) m = RE_JG_64.matcher(uberStr);

            if (m.matches()) {
                int f = Integer.parseInt(m.group(4), 16);
                int flags = DJM_JAEGER | ((0 != (f&1)) ? F_SAMPLE : F_DROP);
                if (0 != (f & 2)) flags |= F_DEBUG;
                DTraceContext ds = dtrace(m.group(1), m.group(2), m.group(3), flags);

                for (Map.Entry<String,Object> e : rec.entrySet()) {
                    String k = e.getKey();
                    if (k.startsWith(DH_UBER_CTX) && e.getValue() != null && ds != null) {
                        ds.getBaggage().put(k.substring(DH_UBER_CTX.length()), e.getValue().toString());
                    }
                }
                return ds;
            }
        }

        log.warn("Cannot parse jaeger context ctx: " + uberStr);
        return null;
    }


    private DTraceContext parseW3Ctx(Map<String,Object> rec) {
        String pidStr = (String)rec.get(DH_W3_TRACEPARENT);

        if (pidStr != null) {
            Matcher m = RE_W3.matcher(pidStr);
            if (m.matches()) {
                int flags = DFM_W3C;
                int f = Integer.parseInt(m.group(4), 16);
                if (0 != (f & 0x01)) flags |= F_SAMPLE;
                DTraceContext ds = dtrace(m.group(2), null, m.group(3), flags);
                if (ds != null) {
                    ds.setTraceState((String)rec.get(DH_W3_TRACESTATE));
                    ds.setSpanId(rand.nextLong());
                }
                return ds;
            }
        }

        log.warn("Cannot parse jaeger context ctx: " + pidStr);
        return null;
    }


    private DTraceContext newCtx() {
        return new DTraceContext(rand.nextLong(), rand.nextLong(), 0, rand.nextLong(),
                System.currentTimeMillis(), defFlags);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> rec) {

        DTraceContext ds = tracer.getHandler().parentDTraceState();

        if (ds != null) {
            ds = new DTraceContext(ds);
            ds.setTstart(System.currentTimeMillis());
        } else if (rec.containsKey(DTRACE_STATE)) {
            ds = new DTraceContext((DTraceContext)rec.get(DTRACE_STATE));
        } else if (rec.containsKey(DH_B3_TRACEID)) {
            ds = parseZipkinCtx(rec);
        } else if (rec.containsKey(DH_B3)) {
            ds = parseZipkinB3Ctx(rec);
        } else if (rec.containsKey(DH_UBER_TID)) {
            ds = parseJaegerCtx(rec);
        } else if (rec.containsKey(DH_W3_TRACEPARENT)) {
            ds = parseW3Ctx(rec);
        }

        if (ds == null) {
            ds = newCtx();
        } else {
            ds.setParentId(ds.getSpanId());
            ds.setSpanId(rand.nextLong());
        }

        // TODO tutaj ew. decyzja samplera
        ds.setFlags(ds.getFlags()|addFlags);

        rec.put(DTRACE_STATE, ds);
        tracer.getHandler().setDTraceState(ds);
        tracerLib.newAttr(DT_TRACE_ID, ds.getTraceIdHex());
        tracerLib.newAttr(DT_SPAN_ID, ds.getSpanIdHex());
        if (ds.getParentId() != 0) tracerLib.newAttr(DT_PARENT_ID, ds.getParentIdHex());

        // TODO tutaj submit danych do odpowiednich backendów, np. zipkin, jaeger itd. (do zico idzie normalnym tracerem)

        return rec;
    }

}
