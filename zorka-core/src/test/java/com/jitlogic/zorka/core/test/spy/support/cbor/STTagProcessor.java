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

package com.jitlogic.zorka.core.test.spy.support.cbor;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.st.STraceHandler;
import com.jitlogic.zorka.cbor.TagProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.util.ZorkaUnsafe.*;
import static com.jitlogic.zorka.cbor.TraceDataFormat.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class STTagProcessor implements TagProcessor {

    private SymbolRegistry registry;

    public STTagProcessor(SymbolRegistry registry) {
        this.registry = registry;
    }

    private long unpackLong(byte[] buf, int offs) {
        if (buf.length-offs >= 8) {
            return UNSAFE.getLong(buf, BYTE_ARRAY_OFFS+offs);
        } else {
            fail("Not enough buffer space for long int.");
        }
        return 0;

    }

    @Override
    public Object process(int tag, Object obj) {
        if (tag == TAG_TRACE_START) {
            Map<String,Object> attrs = new HashMap<String, Object>();
            List<STAttr> uattrs = new ArrayList<STAttr>();
            List<STRec> children = new ArrayList<STRec>();
            STRec rec = new STRec(attrs, uattrs, children);
            List<Object> lst = (List<Object>)obj;
            if (lst.size() < 2) {
                fail("Incomplete trace record.");
            }

            // Decode trace record prolog data
            Object obj0 = lst.get(0);
            if (obj0 instanceof byte[]) {
                long l = unpackLong((byte[]) obj0,0);
                rec.setTstart(l & STraceHandler.TSTAMP_MASK);
                int mid = (int)(l >> STraceHandler.TSTAMP_BITS);
                rec.setMid(mid);
                int[] md = registry.methodDef(mid);
                if (md != null) {
                    rec.setClassId(md[0]);
                    rec.setClassName(registry.symbolName(md[0]));
                    rec.setMethodId(md[1]);
                    rec.setMethodName(registry.symbolName(md[1]));
                    rec.setSignatureId(md[2]);
                    rec.setSignature(registry.symbolName(md[2]));
                }
            } else {
                fail("Trace record prolog should be a byte array.");
            }

            // Decode trace record epilog data
            Object objN = lst.get(lst.size()-1);
            if (objN instanceof byte[]) {
                long l = unpackLong((byte[]) objN, 0);
                rec.setTstop(l & STraceHandler.TSTAMP_MASK);
                rec.setCalls(l >> STraceHandler.TSTAMP_BITS);
            } else {
                fail("Trace record epilog should be a byte array.");
            }

            // Attach remaining objects
            for (Object o : lst.subList(1, lst.size()-1)) {
                if (o instanceof STBeg) {
                    rec.setBegin((STBeg)o);
                } else if (o instanceof STRec) {
                    children.add((STRec)o);
                } else if (o instanceof Map) {
                    attrs.putAll((Map<String,Object>)o);
                } else if (o instanceof STErr) {
                    rec.setError((STErr)o);
                } else if (o instanceof STAttr) {
                    STAttr a = (STAttr)o;
                    uattrs.add(a);
                }
            }

            return rec;
        } else if (tag == TAG_TRACE_BEGIN) {
            STBeg begin = new STBeg();
            List<Object> lst = (List<Object>)obj;
            if (lst.size() < 2) {
                fail("Incomplete trace BEGIN marker.");
            }
            begin.setTraceClock((Long)(lst.get(0)));
            begin.setTraceId(((Integer)(lst.get(1))));
            begin.setTraceName(registry.symbolName(begin.getTraceId()));
            return begin;
        } else if (tag == TAG_TRACE_ATTR) {
            Map<Object,Object> m = (Map)obj;
            Map<String,Object> rslt = new HashMap<String, Object>();
            translateMap(m, rslt);
            return rslt;
        } else if (tag == TAG_TRACE_UP_ATTR) {
            List<Object> lst = (List<Object>)obj;
            assertEquals("UP_ATTR array should contain 2 attributes.", 2, lst.size());
            Integer tid = (Integer)lst.get(0);
            Map<Object,Object> m = (Map<Object,Object>)lst.get(1);
            Map<String,Object> rslt = new HashMap<String, Object>();
            translateMap(m, rslt);
            return new STAttr(tid, registry.symbolName(tid), rslt);
        } else if (tag == TAG_EXCEPTION) {
            fail("To be implemented.");
        } else if (tag == TAG_EXCEPTION_REF) {
            fail("To be implemented.");
        } else if (tag == TAG_PROLOG_BE || tag == TAG_PROLOG_LE || tag == TAG_EPILOG_BE || tag == TAG_EPILOG_LE) {
            // Nothing interesting here ...
            return obj;
        } else if (tag == TAG_STRING_REF) {
            return obj;
        } else {
            fail(String.format("Illegal tag: 0x%x", tag));
        }
        return obj;
    }

    private void translateMap(Map<Object, Object> m, Map<String, Object> rslt) {
        for (Map.Entry e : m.entrySet()) {
            int i = (Integer)(e.getKey());
            String k = registry.symbolName(i);
            if (k == null) {
                fail("Illegal attribute key: " + i);
            }
            rslt.put(k, e.getValue());
        }
    }
}
