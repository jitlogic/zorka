/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.core.spy.output;

import com.jitlogic.zorka.cbor.CBOR;
import com.jitlogic.zorka.cbor.CborDataWriter;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.cbor.CborResendException;

import static com.jitlogic.zorka.cbor.TraceDataTags.*;
import static com.jitlogic.zorka.cbor.TraceRecordFlags.*;
import static com.jitlogic.zorka.cbor.TextIndexTypeMarkers.*;

import java.util.*;

/**
 * Tracer output sending data to remote ZICO collector using ZICO2+CBOR protocol.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class LTraceHttpOutput extends ZicoHttpOutput {

    private static final int C_SHIFT = 0;
    private static final int M_SHIFT = 21;
    private static final int S_SHIFT = 42;

    private static final int K_MASK = 0x001fffff;

    private int lastMid = 0, nExceptions;
    private Map<Long,Integer> mids = new HashMap<Long, Integer>();

    private BitVector symbolsSent = new BitVector();

    private int TBUFSZ = 512 * 1024, ABUFSZ = 128 * 1024;

    private CborDataWriter awriter = new CborDataWriter(ABUFSZ, ABUFSZ), twriter = new CborDataWriter(TBUFSZ, TBUFSZ);

    public LTraceHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry) {
        super(config, conf, registry);
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
                awriter.writeTag(TAG_STRING_DEF);
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
        int classId = ref(tr.getClassId(), CLASS_TYPE);
        int methodId = ref(tr.getMethodId(), METHOD_TYPE);
        int signatureId = ref(tr.getSignatureId(), SIGN_TYPE);

        long key = cms2key(classId, methodId, signatureId);

        Integer mid = mids.get(key);

        if (mid == null) {
            lastMid++;
            mid = lastMid;
            mids.put(key, mid);

            awriter.writeTag(TAG_METHOD_DEF);
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
        twriter.writeTag(TAG_TRACE_START);
        twriter.write(CBOR.ARR_VCODE);

        // Trace Prolog
        twriter.writeTag(TAG_PROLOG_BE);
        twriter.writeUInt(CBOR.BYTES_BASE, 8);
        long prolog = ((t >>> 16) & 0xFFFFFFFFFFL) | (methodId << 40);
        twriter.writeRawLong(prolog, false);


        // Trace Marker (if this is trace beginning)
        if (tr.hasFlag(TraceRecord.TRACE_BEGIN)) {
            TraceMarker tm = tr.getMarker();
            int tid = ref(tm.getTraceId(), STRING_TYPE);
            twriter.writeTag(TAG_TRACE_BEGIN);
            int l = 2;
            DTraceContext ds = tm.getDstate();
            if (ds != null) {
                if (ds.getSpanId() != 0) l++;
                if (ds.getParentId() != 0) l++;
            }
            twriter.writeUInt(CBOR.ARR_BASE, l);
            twriter.writeLong(tm.getClock());
            twriter.writeInt(tid);
            if (ds != null) {
                if (ds.getSpanId() != 0) twriter.writeLong(ds.getSpanId());
                if (ds.getParentId() != 0) twriter.writeLong(ds.getParentId());
            }
            if (tm.hasFlag(TraceMarker.ERROR_MARK)) {
                twriter.writeTag(TAG_TRACE_FLAGS);
                twriter.writeInt(TF_ERROR_MARK);
            }
        }

        Map<Integer,Object> attrs = tr.getAttrs();

        // Attributes (if any)
        if (attrs != null) {
            twriter.writeTag(TAG_TRACE_ATTR);
            twriter.writeUInt(CBOR.MAP_BASE, attrs.size());
            for (Map.Entry<Integer,Object> e : attrs.entrySet()) {
                twriter.writeTag(TAG_STRING_REF); // TODO get rid of this tag
                twriter.writeInt(ref(e.getKey(), STRING_TYPE));
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
            twriter.writeTag(TAG_EXCEPTION);
            twriter.writeUInt(CBOR.ARR_BASE, 5);
            twriter.writeInt(++nExceptions);    // just allocating sequential exception IDs for now
            twriter.writeTag(TAG_STRING_REF);
            twriter.writeInt(ref(se.getClassId(), CLASS_TYPE));
            twriter.writeString(se.getMessage());
            twriter.writeInt(0); // TODO generate proper CauseID
            twriter.writeUInt(CBOR.ARR_BASE, se.getStackTrace().length);
            for (SymbolicStackElement el : se.getStackTrace()) {
                twriter.writeUInt(CBOR.ARR_BASE, 4);
                twriter.writeInt(ref(el.getClassId(), CLASS_TYPE));
                twriter.writeInt(ref(el.getMethodId(), METHOD_TYPE));
                twriter.writeInt(ref(el.getFileId(), STRING_TYPE));
                twriter.writeInt(el.getLineNum() >= 0 ? el.getLineNum() : 0);
            }
        }

        // Epilog
        t += tr.getTime();
        long calls = tr.getCalls() < 0x1000000 ? tr.getCalls() : 0;
        twriter.writeTag(TAG_EPILOG_BE);
        twriter.writeULong(CBOR.BYTES_BASE, calls != 0 ? 8 : 16);
        twriter.writeRawLong(((t >>> 16) & 0xFFFFFFFFFFL) | (calls << 40), false);
        if (calls == 0) {
            twriter.writeRawLong(tr.getCalls(), false);
        }

        // Array Finish
        twriter.write(CBOR.BREAK_CODE);

    } // processTraceRecord()


    private void resetState() {
        symbolsSent.reset();
        mids.clear();
        lastMid = 0;
        isClean = true;
    }


    @Override
    protected void process(List<SymbolicRecord> obj) {
        for (SymbolicRecord sr : obj) {
            long rt = retryTime;
            for (int i = 0; i < retries+1; i++) {
                try {
                    awriter.reset();
                    twriter.reset();
                    nExceptions = 0;

                    if (sr instanceof TraceRecord) {
                        TraceRecord tr = (TraceRecord) sr;
                        processTraceRecord(0, tr);
                    }

                    if (awriter.position() > 0) {
                        byte[] data = ZorkaUtil.clipArray(awriter.getBuf(), awriter.position()); // TODO get rid of this allocation
                        send(data, data.length, submitAgentUrl, 0, 0, isClean);
                        isClean = false;
                    }

                    if (twriter.position() > 0) {
                        long traceId1 = 0, traceId2 = 0;
                        if (sr instanceof TraceRecord) {
                            TraceMarker tm = ((TraceRecord)sr).getMarker();
                            if (tm.getDstate() != null) {
                                traceId1 = tm.getDstate().getTraceId1();
                                traceId2 = tm.getDstate().getTraceId2();
                            } else {
                                traceId1 = rand.nextLong();
                                traceId2 = rand.nextLong();
                            }
                        }
                        byte[] data = ZorkaUtil.clipArray(twriter.getBuf(), twriter.position()); // TODO get rid of this allocation
                        send(data, data.length, submitTraceUrl, traceId1, traceId2, false);
                    }

                    break;

                } catch (CborResendException e) {
                    log.info("Session expired. Reauthenticating ...");
                    resetState();
                } catch (Exception e) {
                    log.error("Error sending trace record: " + e + ". Resetting connection.", e);
                    resetState();
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
