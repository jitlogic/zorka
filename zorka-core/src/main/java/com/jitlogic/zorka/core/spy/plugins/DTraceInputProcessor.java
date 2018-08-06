/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TracerLib;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

/**
 *
 */
public class DTraceInputProcessor implements SpyProcessor {

    private long threshold;
    private TracerLib tracer;
    private ThreadLocal<DTraceState> dtraceLocal;

    private Pattern RE_DIGIT = Pattern.compile("\\d+");

    public DTraceInputProcessor(TracerLib tracer, ThreadLocal<DTraceState> dtraceLocal, long threshold) {
        this.tracer = tracer;
        this.dtraceLocal = dtraceLocal;
        this.threshold = threshold;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {

        String uuid = (String)rec.get(DTRACE_UUID);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        tracer.newAttr(DTRACE_UUID, uuid);

        String tid = (String)rec.get(DTRACE_IN);
        if (tid == null) {
            tid = "";
        }

        String s = (String)rec.get(DTRACE_XTT);

        long t = s != null && RE_DIGIT.matcher(s).matches() ? Long.parseLong(s) : threshold;

        Long tstart = (Long)rec.get("T1");

        if (tstart == null) {
            tstart = System.currentTimeMillis();
        }

        DTraceState ds = new DTraceState(tracer, uuid, tid, tstart, t);
        rec.put(DTRACE_STATE, ds);

        dtraceLocal.set(ds);

        tracer.newAttr(DTRACE_UUID, uuid);
        tracer.newAttr(DTRACE_IN, uuid+tid);

        return rec;
    }

}
