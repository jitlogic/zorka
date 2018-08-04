/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.http.HttpRequest;
import com.jitlogic.zorka.common.http.HttpResponse;
import com.jitlogic.zorka.common.http.HttpUtil;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.common.zico.CborResendException;
import com.jitlogic.zorka.common.zico.TraceDataFormat;
import com.jitlogic.zorka.common.zico.ZicoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;

import static com.jitlogic.zorka.common.util.ZorkaConfig.*;

/**
 * Tracer output sending data to remote ZICO collector using ZICO2+CBOR protocol.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class CborTraceOutput extends ZorkaAsyncThread<SymbolicRecord> {

    private static Logger log = LoggerFactory.getLogger(CborTraceOutput.class);

    private static final int C_SHIFT = 0;
    private static final int M_SHIFT = 21;
    private static final int S_SHIFT = 42;

    private static final int K_MASK = 0x001fffff;

    private String agentUUID, authKey;
    private String app, env, hostname;
    private String sessionUUID, sessionKey;

    private String submitTraceUrl, submitAgentUrl, registerUrl, sessionUrl;

    private int retries, timeout;
    private long retryTime, retryTimeExp;

    private int lastMid = 0, nExceptions;
    private Map<Long,Integer> mids = new HashMap<Long, Integer>();

    private BitVector symbolsSent = new BitVector();

    private int TBUFSZ = 512 * 1024, ABUFSZ = 128 * 1024;

    private CborDataWriter awriter = new CborDataWriter(ABUFSZ, ABUFSZ), twriter = new CborDataWriter(TBUFSZ, TBUFSZ);
    private SymbolRegistry registry;


    private ZorkaConfig config;

    public CborTraceOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry) {

        super("ZORKA-CBOR-OUTPUT", parseInt(conf.get("http.qlen"), 64, "tracer.http.qlen"), 1);

        this.config = config;

        this.agentUUID = conf.get("agent.uuid");

        this.hostname = parseStr(conf.get("hostname"), null, null,
                "CborTraceOutput: missing mandatory parameter: tracer.hostname");

        this.app = parseStr(conf.get("app.name"), null, null,
                "CborTraceOutput: missing mandatory parameter: tracer.app.name");

        this.env = parseStr(conf.get("env.name"), null, null,
                "CborTraceOutput: missing mandatory parameter: tracer.env.name");

        this.authKey = conf.get("auth.key");
        this.sessionKey = conf.get("sessn.key");

        String url = parseStr(conf.get("http.url"), null, null,
                "CborTraceOutput: missing mandatory parameter: tracer.http.url");

        if (!url.endsWith("/")) url = url + "/";
        url += "agent/";

        this.submitAgentUrl = url + "submit/agd";
        this.submitTraceUrl = url + "submit/trc";

        this.registerUrl = url + "register";
        this.sessionUrl = url + "session";

        this.registry = registry;

        this.retries = parseInt(conf.get("http.retries"), 10, "tracer.http.retries");
        this.retryTime = parseInt(conf.get("http.retry.time"), 125, "tracer.http.retry.time");
        this.retryTimeExp = parseInt(conf.get("http.retry.exp"), 2, "tracer.http.retry.exp");
        this.timeout = parseInt(conf.get("http.timeout"), 60000, "tracer.http.output");
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
            int tid = ref(tm.getTraceId(), TraceDataFormat.STRING_TYPE);
            twriter.writeTag(TraceDataFormat.TAG_TRACE_BEGIN);
            twriter.writeUInt(CBOR.ARR_BASE, 2);
            twriter.writeLong(tm.getClock());
            twriter.writeInt(tid);
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

    public void register() {
        symbolsSent.reset();
        mids.clear();
        lastMid = 0;
        sessionUUID = null;

        String json = new JSONWriter()
                .write(ZorkaUtil.map(
                        "rkey", authKey,
                        "name", hostname,
                        "app", app,
                        "env", env
                ));

        log.info("Registering agent as: name=" + hostname + " app=" + app + " env=" + env);

        try {
            HttpRequest req = HttpUtil.POST(registerUrl, json)
                    .withHeader("Content-Type", "application/json");
            HttpResponse res = req.go();
            if (res.getStatus() == 201 || res.getStatus() == 200) {
                Object rslt = new JSONReader().read(res.getBodyAsString());
                if (rslt instanceof Map) {
                    Map m = (Map)rslt;
                    Object u = m.get("uuid");
                    if (u instanceof String) {
                        agentUUID = (String) m.get("uuid");
                        sessionKey = (String) m.get("authkey");
                        config.writeCfg("tracer.net.agent.uuid", agentUUID);
                        config.writeCfg("tracer.net.sessn.key", sessionKey);
                        log.info("Successfully registered agent with uuid=" + agentUUID);
                    } else {
                        throw new ZorkaRuntimeException("Invalid registration response from collector: missing or bad UUID '" + res.getBodyAsString() + "'");
                    }
                } else {
                    throw new ZorkaRuntimeException("Invalid registration response from collector: '" + res.getBodyAsString() + "'");
                }
            } else {
                throw new ZorkaRuntimeException("Invalid registration response from collector: "
                        + res.getStatus() + ": " + res.getStatusMsg());
            }
        } catch (IOException e) {
            throw new ZorkaRuntimeException("I/O error occured while registering agent", e);
        }
    }

    public String getAgentUUID() {
        return agentUUID;
    }

    public void openSession() {
        symbolsSent.reset();
        mids.clear();
        lastMid = 0;
        sessionUUID = null;

        try {
            if (agentUUID == null) {
                log.info("Agent not registered (yet). Registering ...");
                register();
            }
        } catch (ZorkaRuntimeException e) {
            log.error("Error registering agent.", e);
            throw e;
        }

        try {
            log.debug("Requesting session for agent: uuid=" + agentUUID);
            HttpRequest req = HttpUtil.POST(sessionUrl,
                    new JSONWriter().write(ZorkaUtil.map(
                            "uuid", agentUUID,
                            "authkey", sessionKey)));
            HttpResponse res = req.go();
            if (res.getStatus() == 200) {
                Object rslt = new JSONReader().read(res.getBodyAsString());
                if (rslt instanceof Map) {
                    Map m = (Map)rslt;
                    Object s = m.get("session");
                    if (s instanceof String) {
                        sessionUUID = (String)s;
                        log.debug("Obtained session: uuid=" + sessionUUID);
                    } else {
                        throw new ZorkaRuntimeException("Invalid session response: '" + res.getBodyAsString() + "'");
                    }
                } else {
                    throw new ZorkaRuntimeException("Invalid session response: '" + res.getBodyAsString() + "'");
                }
            } else if (res.getStatus() == 401) {
                throw new ZorkaRuntimeException("Not authorized. Check trapper.cbor.auth-key property.");
            } else {
                throw new ZorkaRuntimeException("Server error: " + res.getStatus() + " " + res.getStatusMsg());
            }
        } catch (IOException e) {
            throw new ZorkaRuntimeException("I/O exception: " + e.getMessage(), e);
        }
    }

    public void newSession() {
        long rt = retryTime;
        for (int i = 0; i < retries; i++) {
            try {
                openSession();

                if (sessionUUID != null) {
                    break;
                }
            } catch (Exception e) {
                log.error("Cannot open collector session to: " + sessionUrl, e);
            }

            rt *= retryTimeExp;

            try {
                Thread.sleep(rt);
            } catch (InterruptedException e) {
            }
        }

        if (sessionUUID == null) {
            throw new ZorkaRuntimeException("Cannot obtain agent session.");
        }
    }

    private void send(CborDataWriter w, String uri, String traceUUID) {
        try {
            // TODO this is inefficient, implement dedicated Base64/json/etc encoding in HttpClient
            HttpRequest req = HttpUtil.POST(uri, DatatypeConverter.printBase64Binary(Arrays.copyOf(w.getBuf(), w.position())));
            req.setHeader("X-Zorka-Agent-UUID", agentUUID);
            req.setHeader("X-Zorka-Session-UUID", sessionUUID);
            req.setHeader("Content-Type", "application/zorka+cbor+v1");
            if (traceUUID != null) req.setHeader("X-Zorka-Trace-UUID", traceUUID);
            HttpResponse res = req.go();
            if (res.getStatus() < 300) {
                log.trace("Submitted: " + uri + " : " + traceUUID);
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

                    if (sessionUUID == null) newSession();

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
                    log.info("Session expired. Reauthenticating ...");
                    newSession();
                } catch (Exception e) {
                    log.error("Error sending trace record: " + e + ". Resetting connection.", e);
                    newSession();
                }

                try {
                    log.debug("Will retry (wait=" + rt + ")");
                    Thread.sleep(rt);
                } catch (InterruptedException e) {
                    log.warn("Huh? Sleep interrupted?");
                }

                rt *= retryTimeExp;
            } // for
        }
    }
}
