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
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTracer implements TraceEventHandler {

    private List<Map<Object,Object>> data = new ArrayList<Map<Object, Object>>();

    @Override
    public void newTrace(int traceId, long tstamp) {
        data.add(ZorkaUtil.map("type", "newTrace", "traceId", traceId, "tstamp", tstamp));
    }

    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        data.add(ZorkaUtil.map("type", "traceEnter", "classId", classId, "methodId", methodId, "signatureId", signatureId, "tstamp", tstamp));
    }

    @Override
    public void traceReturn(long tstamp) {
        data.add(ZorkaUtil.map("type", "traceReturn", "tstamp", tstamp));
    }

    @Override
    public void traceError(Throwable exception, long tstamp) {
        data.add(ZorkaUtil.map("type", "traceError", "exception", exception, "tstamp", tstamp));
    }

    @Override
    public void newSymbol(int symbolId, String symbolText) {
        data.add(ZorkaUtil.map("type", "newSymbol", "symbolId", symbolId, "symbolText", symbolText));
    }

    @Override
    public void newParam(int parId, String val) {
        data.add(ZorkaUtil.map("type", "newParam", "parId", parId, "val", val));
    }

    @Override
    public void newParam(int parId, int val) {
        data.add(ZorkaUtil.map("type", "newParam", "parId", parId, "val", val));
    }

    @Override
    public void newParam(int parId, long val) {
        data.add(ZorkaUtil.map("type", "newParam", "parId", parId, "val", val));
    }

    @Override
    public void newParam(int parId, double val) {
        data.add(ZorkaUtil.map("type", "newParam", "parId", parId, "val", val));
    }

    public List<Map<Object,Object>> getData() {
        return data;
    }
}
