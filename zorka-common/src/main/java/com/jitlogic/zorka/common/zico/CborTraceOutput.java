package com.jitlogic.zorka.common.zico;

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.*;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;

/**
 * Tracer output sending data to remote ZICO collector using ZICO2+CBOR protocol.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class CborTraceOutput extends ZorkaAsyncThread<SymbolicRecord> {

    private static ZorkaLog log = ZorkaLogger.getLog(CborTraceOutput.class);

    private static final int C_SHIFT = 0;
    private static final int M_SHIFT = 21;
    private static final int S_SHIFT = 42;

    private static final int K_MASK = 0x001fffff;

    private String agentUUID, authKey;
    private String app, env, hostname;
    private String sessionUUID;

    private String submitTraceUrl, submitAgentUrl, authUrl;

    private int retries, timeout;
    private long retryTime, retryTimeExp;

    private int lastMid = 0, nExceptions;
    private Map<Long,Integer> mids = new HashMap<Long, Integer>();

    private BitVector symbolsSent = new BitVector();

    private int TBUFSZ = 512 * 1024, ABUFSZ = 128 * 1024;

    private CborDataWriter awriter = new CborDataWriter(ABUFSZ, ABUFSZ), twriter = new CborDataWriter(TBUFSZ, TBUFSZ);
    private SymbolRegistry registry;

    private Map<String,Integer> traceTypes;


    public CborTraceOutput(String url, String agentUUID, String authKey,
                           String hostname, String app, String env,
                           SymbolRegistry registry, Map<String,Integer> traceTypes,
                           int qlen, int retries, long retryTime, long retryTimeExp, int timeout) {
        super("ZORKA-CBOR-TRACE-OUTPUT", qlen, 1);

        this.agentUUID = agentUUID;
        this.authKey = authKey;
        this.hostname = hostname;
        this.app = app;
        this.env = env;

        if (!url.endsWith("/")) url = url + "/";
        url += "agent/";

        this.submitAgentUrl = url + "submit/agd";
        this.submitTraceUrl = url + "submit/trc";
        this.authUrl = url + "auth";

        this.registry = registry;
        this.traceTypes = traceTypes;

        this.retries = retries;
        this.retryTime = retryTime;
        this.retryTimeExp = retryTimeExp;
        this.timeout = timeout;
    }

    private static long cms2key(int classId, int methodId, int signatureId) {
        return ((long)classId) | (((long)methodId) << M_SHIFT) | (((long)signatureId) << S_SHIFT);
    }

    private static int key2cms(long cms, int shift) {
        return (int)((cms >>> shift) & K_MASK);
    }

    private int ref(int id, int type) {

        if (!symbolsSent.get(id)) {
            String s = registry.symbolName(id);
            if (s != null) {
                awriter.writeTag(TraceDataFormat.TAG_STRING_DEF);
                awriter.writeUInt(CBOR.ARR_BASE, 3);
                awriter.writeInt(id);
                awriter.writeString(s);
                awriter.writeInt(type);
            }
            symbolsSent.set(id);
        }

        return id;
    }

    private int mid(TraceRecord tr) {
        int classId = ref(tr.getClassId(), TraceDataFormat.CLASS_TYPE);
        int methodId = ref(tr.getMethodId(), TraceDataFormat.METHOD_TYPE);
        int signatureId = ref(tr.getSignatureId(), TraceDataFormat.SIGN_TYPE);

        long key = cms2key(classId, methodId, signatureId);

        Integer mid = mids.get(key);

        if (mid == null) {
            lastMid++;
            mid = lastMid;
            mids.put(key, mid);

            awriter.writeTag(TraceDataFormat.TAG_METHOD_DEF);
            awriter.writeUInt(CBOR.ARR_BASE, 4);
            awriter.writeInt(mid);
            awriter.writeInt(classId);
            awriter.writeInt(methodId);
            awriter.writeInt(signatureId);
        }

        return mid;
    }

    private void processTraceRecord(long t, TraceRecord tr) {
        long methodId = mid(tr);

        // Leading tag and Array Start
        twriter.writeTag(TraceDataFormat.TAG_TRACE_START);
        twriter.write(CBOR.ARR_VCODE);

        // Trace Prolog
        twriter.writeTag(TraceDataFormat.TAG_PROLOG_BE);
        twriter.writeUInt(CBOR.BYTES_BASE, 8);
        long prolog = ((t >>> 16) & 0xFFFFFFFFFFL) | (methodId << 40);
        twriter.writeRawLong(prolog, false);


        // Trace Marker (if this is trace beginning)
        if (tr.hasFlag(TraceRecord.TRACE_BEGIN)) {
            TraceMarker tm = tr.getMarker();
            String traceType = registry.symbolName(tm.getTraceId());
            if (traceType != null) {
                Integer tid = traceTypes.get(traceType);
                if (tid != null) {
                    twriter.writeTag(TraceDataFormat.TAG_TRACE_BEGIN);
                    twriter.writeUInt(CBOR.ARR_BASE, 2);
                    twriter.writeLong(tm.getClock());
                    twriter.writeInt(tid);
                } else {
                    log.error(ZorkaLogger.ZTR_ERRORS, "Mapping for trace type '" + traceType + "' not found. "
                        + "Use tracer.defType() function to define mapping for CBOR output.");
                }
            } else {
                log.error(ZorkaLogger.ZTR_ERRORS, "Symbol name for typeId '" + tm.getTraceId()
                    + "' not found. Internal error. (?)");
            }
            if (tm.hasFlag(TraceMarker.ERROR_MARK)) {
                twriter.writeTag(TraceDataFormat.TAG_FLAG_TOKEN);
                twriter.writeInt(TraceDataFormat.FLAG_ERROR);
            }
        }

        Map<Integer,Object> attrs = tr.getAttrs();

        // Attributes (if any)
        if (attrs != null) {
            twriter.writeTag(TraceDataFormat.TAG_TRACE_ATTR);
            twriter.writeUInt(CBOR.MAP_BASE, attrs.size());
            for (Map.Entry<Integer,Object> e : attrs.entrySet()) {
                twriter.writeTag(TraceDataFormat.TAG_STRING_REF);
                twriter.writeInt(ref(e.getKey(), TraceDataFormat.STRING_TYPE));
                Object v = e.getValue();
                if (v == null) {
                    twriter.writeNull();
                } else if (v.getClass() == String.class) {
                    twriter.writeString((String)v);
                } else if (v.getClass() == Integer.class || v.getClass() == Long.class
                    || v.getClass() == Short.class || v.getClass() == Byte.class) {
                    twriter.writeLong(((Number)v).longValue());
                } else {
                    twriter.writeString(v.toString());
                }
            }
        }

        long dt = 0;

        List<TraceRecord> ctrs = tr.getChildren();

        // Child trace records (if any)
        if (ctrs != null) {
            for (TraceRecord ctr : ctrs) {
                processTraceRecord(t + dt, ctr);
                dt += ctr.getTime();
            }
        }

        if (tr.getException() != null) {
            SymbolicException se = (SymbolicException)(tr.getException());
            twriter.writeTag(TraceDataFormat.TAG_EXCEPTION);
            twriter.writeUInt(CBOR.ARR_BASE, 5);
            twriter.writeInt(++nExceptions);    // just allocating sequential exception IDs for now
            twriter.writeTag(TraceDataFormat.TAG_STRING_REF);
            twriter.writeInt(ref(se.getClassId(), TraceDataFormat.CLASS_TYPE));
            twriter.writeString(se.getMessage());
            twriter.writeInt(0); // TODO generate proper CauseID
            twriter.writeUInt(CBOR.ARR_BASE, se.getStackTrace().length);
            for (SymbolicStackElement el : se.getStackTrace()) {
                twriter.writeUInt(CBOR.ARR_BASE, 4);
                twriter.writeInt(ref(el.getClassId(), TraceDataFormat.CLASS_TYPE));
                twriter.writeInt(ref(el.getMethodId(), TraceDataFormat.METHOD_TYPE));
                twriter.writeInt(ref(el.getFileId(), TraceDataFormat.STRING_TYPE));
                twriter.writeInt(el.getLineNum() >= 0 ? el.getLineNum() : 0);
            }
        }

        // Epilog
        t += tr.getTime();
        long calls = tr.getCalls() < 0x1000000 ? tr.getCalls() : 0;
        twriter.writeTag(TraceDataFormat.TAG_EPILOG_BE);
        twriter.writeULong(CBOR.BYTES_BASE, calls != 0 ? 8 : 16);
        twriter.writeRawLong(((t >>> 16) & 0xFFFFFFFFFFL) | (calls << 40), false);
        if (calls == 0) {
            twriter.writeRawLong(tr.getCalls(), false);
        }

        // Array Finish
        twriter.write(CBOR.BREAK_CODE);

    } // processTraceRecord()

    public void authenticate() {
        symbolsSent.reset();
        mids.clear();
        lastMid = 0;
        sessionUUID = null;

        try {
            HttpRequest req = HttpUtil.GET(authUrl);
            req.setHeader("X-ZICO-Agent-UUID", agentUUID);
            req.setHeader("X-ZICO-Auth-Key", authKey);
            req.setParam("env", env);
            req.setParam("app", app);
            HttpResponse res = req.go();
            if (res.getStatus() == 200) {
                sessionUUID = res.getBodyAsString();
            } else if (res.getStatus() == 401) {
                throw new ZorkaRuntimeException("Not authorized. Check trapper.cbor.auth-key property.");
            } else {
                throw new ZorkaRuntimeException("Server error: " + res.getStatus() + " " + res.getStatusMsg());
            }
        } catch (IOException e) {
            throw new ZorkaRuntimeException("I/O exception: " + e.getMessage(), e);
        }
    }

    private void send(CborDataWriter w, String uri, String traceUUID) {
        try {
            // TODO this is inefficient, implement dedicated Base64/json/etc encoding in HttpClient
            HttpRequest req = HttpUtil.POST(uri, DatatypeConverter.printBase64Binary(Arrays.copyOf(w.getBuf(), w.position())));
            req.setHeader("X-ZICO-Agent-UUID", agentUUID);
            req.setHeader("X-ZICO-Session-UUID", sessionUUID);
            if (traceUUID != null) req.setHeader("X-ZICO-Trace-UUID", traceUUID);
            HttpResponse res = req.go();
            if (res.getStatus() < 300) {
                log.trace(ZorkaLogger.ZTR_TRACER_DBG, "Submitted: " + uri + " : " + traceUUID);
            } else if (res.getStatus() == 412) {
                throw new ZorkaRuntimeException("Resend.");
            } else {
               throw new ZorkaRuntimeException("Server error: " + res.getStatus() + " " + res.getStatus());
            }
        } catch (IOException e) {
            throw new ZorkaRuntimeException("I/O exception: " + e.getMessage());
        }
    }

    @Override
    protected void process(List<SymbolicRecord> obj) {
        for (SymbolicRecord sr : obj) {
            long rt = retryTime;
            for (int i = 0; i < retries; i++) {
                try {
                    awriter.reset();
                    twriter.reset();
                    nExceptions = 0;

                    if (sessionUUID == null) authenticate();

                    if (sr instanceof TraceRecord) {
                        TraceRecord tr = (TraceRecord) sr;
                        processTraceRecord(0, tr);
                    }

                    if (awriter.position() > 0) {
                        send(awriter, submitAgentUrl, null);
                    }

                    if (twriter.position() > 0) {
                        send(twriter, submitTraceUrl, UUID.randomUUID().toString());
                    }

                    break;

                } catch (CborResendException e) {
                    log.info(ZorkaLogger.ZTR_TRACER_DBG, "Session expired. Reauthenticating ...");
                    authenticate();
                } catch (Exception e) {
                    log.error(ZorkaLogger.ZCL_STORE, "Error sending trace record: " + e + ". Resetting connection.", e);
                    authenticate();
                }

                try {
                    log.debug(ZorkaLogger.ZTR_TRACER_DBG, "Will retry (wait=" + rt + ")");
                    Thread.sleep(rt);
                } catch (InterruptedException e) {
                    log.warn(ZorkaLogger.ZTR_TRACER_DBG,"Huh? Sleep interrupted?");
                }

                rt *= retryTimeExp;
            } // for (int i = 0 ....
        }
    }
}
