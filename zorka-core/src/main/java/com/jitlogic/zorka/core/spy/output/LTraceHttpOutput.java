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

import com.jitlogic.zorka.common.cbor.CborDataWriter;
import com.jitlogic.zorka.common.cbor.TraceDataWriter;
import com.jitlogic.zorka.common.http.HttpHandler;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.common.cbor.CborResendException;

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

    private CborDataWriter adw = new CborDataWriter(ABUFSZ, ABUFSZ, false), tdw = new CborDataWriter(TBUFSZ, TBUFSZ, false);
    private TraceDataWriter atdw = new TraceDataWriter(adw), ttdw = new TraceDataWriter(tdw);

    public LTraceHttpOutput(ZorkaConfig config, Map<String,String> conf, SymbolRegistry registry, HttpHandler httpClient) {
        super(config, conf, registry, httpClient);
    }

    private static long cms2key(int classId, int methodId, int signatureId) {
        return ((long)classId) | (((long)methodId) << M_SHIFT) | (((long)signatureId) << S_SHIFT);
    }

    private static int key2cms(long cms, int shift) {
        return (int)((cms >>> shift) & K_MASK);
    }

    private int ref(int id) {

        if (!symbolsSent.get(id)) {
            String s = registry.symbolName(id);
            if (s != null) {
                atdw.stringRef(id, s);
            }
            symbolsSent.set(id);
        }

        return id;
    }

    private int mid(TraceRecord tr) {
        int classId = ref(tr.getClassId());
        int methodId = ref(tr.getMethodId());
        int signatureId = ref(tr.getSignatureId());

        long key = cms2key(classId, methodId, signatureId);

        Integer mid = mids.get(key);

        if (mid == null) {
            lastMid++;
            mid = lastMid;
            mids.put(key, mid);
            atdw.methodRef(mid, classId, methodId, signatureId);
        }

        return mid;
    }

    private void processTraceRecord(long t, TraceRecord tr) {
        ttdw.traceStart(tdw.position(), t, mid(tr));

        TraceMarker tm = tr.getMarker();
        if (tr.hasFlag(TraceRecord.TRACE_BEGIN)) {
            DTraceContext ds = tm.getDstate();
            ttdw.traceBegin(tm.getClock(), ref(tm.getTraceId()),
                ds != null ? ds.getSpanId() : rand.nextLong(),
                ds != null ? ds.getParentId() : 0L);
        }

        Map<Integer,Object> attrs = tr.getAttrs();

        // Attributes (if any)
        if (attrs != null) {
            for (Map.Entry<Integer,Object> e : attrs.entrySet()) {
                ttdw.traceAttr(ref(e.getKey()), ""+e.getValue());
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

            List<int[]> stack = new ArrayList<int[]>(se.getStackTrace().length);

            for (SymbolicStackElement ste : se.getStackTrace()) {
                stack.add(new int[] { ste.getClassId(), ste.getMethodId(), ste.getFileId(), ste.getLineNum() });
            }

            // TODO generate proper causeID and proper binding between nException and causeId
            ttdw.exception(++nExceptions, se.getClassId(), ""+se.getMessage(), 0, stack, null);
        }

        t += tr.getTime();
        ttdw.traceEnd(0, t, tr.getCalls(), tm != null ? tm.getFlags() : 0);
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
                    adw.reset();
                    tdw.reset();
                    nExceptions = 0;

                    if (sr instanceof TraceRecord) {
                        TraceRecord tr = (TraceRecord) sr;
                        processTraceRecord(0, tr);
                    }

                    if (adw.position() > 0) {
                        byte[] data = ZorkaUtil.clipArray(adw.getBuf(), adw.position()); // TODO get rid of this allocation
                        send(data, data.length, submitAgentUrl, 0, 0, isClean);
                        isClean = false;
                    }

                    if (tdw.position() > 0) {
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
                        byte[] data = ZorkaUtil.clipArray(tdw.getBuf(), tdw.position()); // TODO get rid of this allocation
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
