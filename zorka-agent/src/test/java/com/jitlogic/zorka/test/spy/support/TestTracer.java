/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.test.spy.support;

import com.jitlogic.zorka.spy.TraceEventHandler;
import com.jitlogic.zorka.spy.TracedException;
import com.jitlogic.zorka.util.ZorkaUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTracer extends TraceEventHandler {

    private List<Map<Object,Object>> data = new ArrayList<Map<Object, Object>>();

    @Override
    public void traceBegin(int traceId, long clock) {
        data.add(ZorkaUtil.map("action", "traceBegin", "traceId", traceId, "clock", clock));
    }

    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        data.add(ZorkaUtil.map("action", "traceEnter", "classId", classId, "methodId", methodId, "signatureId", signatureId, "tstamp", tstamp));
    }

    @Override
    public void traceReturn(long tstamp) {
        data.add(ZorkaUtil.map("action", "traceReturn", "tstamp", tstamp));
    }

    @Override
    public void traceError(TracedException exception, long tstamp) {
        data.add(ZorkaUtil.map("action", "traceError", "exception", exception, "tstamp", tstamp));
    }

    @Override
    public void traceStats(long calls, long errors) {
        data.add(ZorkaUtil.map("action", "traceStats", "calls", calls, "errors", errors));
    }

    @Override
    public void newSymbol(int symbolId, String symbolName) {
        data.add(ZorkaUtil.map("action", "newSymbol", "symbolId", symbolId, "symbolName", symbolName));
    }

    @Override
    public void newAttr(int attrId, Object attrVal) {
        data.add(ZorkaUtil.map("action", "newAttr", "attrId", attrId, "attrVal", attrVal));
    }

    public List<Map<Object,Object>> getData() {
        return data;
    }


    public <T> List<T> listAttr(Object key) {
        List<T> lst = new ArrayList<T>();
        for (Map<Object,Object> datum : data) {
            lst.add((T)datum.get(key));
        }
        return lst;
    }


    public Object get(int idx, Object key) {
        return idx < data.size() ?  data.get(idx).get(key) : null;
    }


    public void check(int idx, Object... kv) {

        if (idx >= data.size()) {
            Assert.fail("Requested slot " + idx + " but only " + data.size() + " have been recorded.");
        }

        Map<Object,Object> rec = data.get(idx);

        for (int i = 1; i < kv.length; i += 2) {
            Assert.assertEquals("Attribute " + kv[i-1], kv[i], rec.get(kv[i-1]));
        }
    }

    public int size() {
        return data.size();
    }
}
