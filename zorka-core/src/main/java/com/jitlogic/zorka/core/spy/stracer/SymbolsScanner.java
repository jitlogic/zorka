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

package com.jitlogic.zorka.core.spy.stracer;

import com.jitlogic.zorka.common.cbor.CBOR;
import com.jitlogic.zorka.common.cbor.CborDataWriter;
import com.jitlogic.zorka.common.cbor.TagProcessor;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.cbor.TraceDataTags.*;
import static com.jitlogic.zorka.common.cbor.TextIndexTypeMarkers.*;

import static com.jitlogic.zorka.core.util.ZorkaUnsafe.BYTE_ARRAY_OFFS;
import static com.jitlogic.zorka.core.util.ZorkaUnsafe.UNSAFE;

public class SymbolsScanner implements TagProcessor {

    private final static Logger log = LoggerFactory.getLogger(TagProcessor.class);

    private BitVector symbolsSent = new BitVector();
    private BitVector methodsSent = new BitVector();

    private SymbolRegistry registry;

    private static final int ABUFSZ = 128 * 1024;

    private CborDataWriter writer = new CborDataWriter(ABUFSZ, ABUFSZ);

    public SymbolsScanner(SymbolRegistry registry) {
        this.registry = registry;
    }

    public void reset() {
        writer.reset();
        symbolsSent.reset();
        methodsSent.reset();
    }

    public void clear() {
        writer.reset();
    }

    private long unpackLong(byte[] buf, int offs) {
        if (buf.length-offs >= 8) {
            return UNSAFE.getLong(buf, BYTE_ARRAY_OFFS+offs);
        } else {
            return 0;
        }
    }

    private boolean addSymbol(int id, int type) {
        if (symbolsSent.get(id)) return false;
        String s = registry.symbolName(id);
        if (s != null) {
            writer.writeTag(TAG_STRING_DEF);
            writer.writeUInt(CBOR.ARR_BASE, 3);
            writer.writeInt(id);
            writer.writeString(s);
            writer.writeInt(type);
        }
        symbolsSent.set(id);
        return true;
    }

    private boolean addMethod(int mid) {
        if (methodsSent.get(mid)) return false;
        int[] md = registry.methodDef(mid);
        if (md != null) {
            writer.writeTag(TAG_METHOD_DEF);
            writer.writeUInt(CBOR.ARR_BASE, 4);
            writer.writeInt(mid);
            writer.writeInt(md[0]);
            writer.writeInt(md[1]);
            writer.writeInt(md[2]);
        }
        return true;
    }

    @Override
    public Object process(int tag, Object obj) {
        if (tag == TAG_TRACE_START) {
            List<Object> lst = (List<Object>)obj;
            if (lst.size() > 0 && lst.get(0) instanceof byte[]) {
                long l = unpackLong((byte[])lst.get(0), 0);
                int mid = (int) (l >> STraceHandler.TSTAMP_BITS);
                if (!methodsSent.get(mid)) {
                    int[] md = registry.methodDef(mid);
                    if (md != null) {
                        addSymbol(md[0], CLASS_TYPE);
                        addSymbol(md[1], METHOD_TYPE);
                        addSymbol(md[2], SIGN_TYPE);
                    }
                    addMethod(mid);
                }
            }
            return null;
        } else if (tag == TAG_TRACE_ATTR) {
            Map<Object, Object> m = (Map) obj;
            for (Map.Entry e : m.entrySet()) {
                addSymbol((Integer)e.getKey(), STRING_TYPE);
            }
            return null;
        } else if (tag == TAG_TRACE_UP_ATTR) {
            List<Object> lst = (List<Object>)obj;
            addSymbol((Integer)lst.get(0), STRING_TYPE);
            Map<Object,Object> m = (Map<Object,Object>)lst.get(1);
            for (Map.Entry e : m.entrySet()) {
                addSymbol((Integer)e.getKey(), STRING_TYPE);
            }
            return null;
        } else if (tag == TAG_EXCEPTION) {
            List<Object> lst = (List<Object>)obj;
            Integer sid = (Integer) lst.get(1);
            if (sid != null) addSymbol(sid, CLASS_TYPE);
            List<Object> stk = (List<Object>)lst.get(4);
            for (Object o : stk) {
                List<Object> si = (List<Object>)o;
                addSymbol((Integer)si.get(0), CLASS_TYPE);
                addSymbol((Integer)si.get(1), METHOD_TYPE);
                addSymbol((Integer)si.get(2), STRING_TYPE);
            }
            return null;
        } else if (tag == TAG_STRING_REF) {
            addSymbol((Integer)obj, STRING_TYPE);
            return obj;
        }
        return obj;
    }

    public byte[] getBuf() {
        return writer.getBuf();
    }

    public int getPosition() {
        return writer.position();
    }
}
