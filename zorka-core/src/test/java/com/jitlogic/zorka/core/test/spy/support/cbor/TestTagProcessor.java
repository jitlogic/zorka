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

package com.jitlogic.zorka.core.test.spy.support.cbor;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.stracer.STraceHandler;
import com.jitlogic.zorka.cbor.TagProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.util.ZorkaUnsafe.*;
import static com.jitlogic.zorka.cbor.TraceDataTags.*;

import static org.junit.Assert.fail;

public class TestTagProcessor implements TagProcessor {

    // TODO get rid of this (unnecesary) tag processor, migrate STRaceHandlerUnitTest to STTagProcessor.

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
            List<Object> children = new ArrayList<Object>();
            Map<Object,Object> attrs = new HashMap<Object, Object>();
            Map<String,Object> rec = ZorkaUtil.map("_", "T");
            List<Object> lst = (List<Object>)obj;
            if (lst.size() < 2) {
                fail("Incomplete trace record.");
            }
            Object obj0 = lst.get(0);
            if (obj0 instanceof byte[]) {
                long l = unpackLong((byte[]) obj0,0);
                rec.put("tstart", l & STraceHandler.TSTAMP_MASK);
                rec.put("method", l >> STraceHandler.TSTAMP_BITS);
            } else {
                fail("Trace record prolog should be a byte array.");
            }
            Object objN = lst.get(lst.size()-1);

            if (objN instanceof byte[]) {
                long l = unpackLong((byte[]) objN, 0);
                rec.put("tstop", l & STraceHandler.TSTAMP_MASK);
                rec.put("calls", l >> STraceHandler.TSTAMP_BITS);
            } else {
                fail("Trace record epilog should be a byte array.");
            }
            for (Object o : lst.subList(1, lst.size()-1)) {
                if (o instanceof Map) {
                    Map m = (Map)o;
                    if ("B".equals(m.get("_"))) {
                        rec.put("begin", m);
                    } else if ("T".equals(m.get("_"))) {
                        children.add(m);
                    } else if ("A".equals(m.get("_"))) {
                        m.remove("_");
                        attrs.putAll(m);
                    } else if ("E".equals(m.get("_"))) {
                        rec.put("error", m);
                    }
                }
            }
            if (attrs.size() > 0) {
                rec.put("attrs", attrs);
            }
            if (children.size() > 0) {
                rec.put("children", children);
            }
            return rec;
        } else if (tag == TAG_TRACE_BEGIN) {
            Map<String,Object> rec = ZorkaUtil.map("_", "B");
            List<Object> lst = (List<Object>)obj;
            if (lst.size() < 2) {
                fail("Incomplete trace BEGIN marker.");
            }
            rec.put("clock", lst.get(0));
            rec.put("trace", lst.get(1));
            return rec;
        } else if (tag == TAG_TRACE_ATTR) {
            Map<Object, Object> rec = (Map) obj;
            rec.put("_", "A");
            return rec;
        } else if (tag == TAG_TRACE_UP_ATTR) {
            Map<Object, Object> rec = (Map) obj;
            rec.put("_", "a");
            return rec;
        } else if (tag == TAG_EXCEPTION) {
            List<Object> lst = (List<Object>)obj;
            Map<String,Object> rec = ZorkaUtil.map(
                "_", "E", "id", lst.get(0), "class", lst.get(1), "message", lst.get(2), "stack", lst.get(4));
            if ((Integer)lst.get(3) != 0) {
                rec.put("cause", lst.get(3));
            }
            return rec;
        } else if (tag == TAG_EXCEPTION_REF) {
            return ZorkaUtil.map("_", "E", "id", obj);
        }
        return obj;
    }
}
