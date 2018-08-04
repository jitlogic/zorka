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

import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.TracerLib;

import java.util.Map;
import java.util.UUID;

import static com.jitlogic.zorka.core.spy.TracerLib.*;

/**
 *
 */
public class DTraceInputProcessor implements SpyProcessor {

    private TracerLib tracer;
    private ThreadLocal<String> uuidLocal;
    private ThreadLocal<String> tidLocal;

    public DTraceInputProcessor(TracerLib tracer, ThreadLocal<String> uuidLocal, ThreadLocal<String> tidLocal) {
        this.tracer = tracer;
        this.uuidLocal = uuidLocal;
        this.tidLocal = tidLocal;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> rec) {

        String uuid = (String)rec.get(DTRACE_UUID);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            rec.put(DTRACE_UUID, uuid);
        }

        tracer.newAttr(DTRACE_UUID, uuid);
        uuidLocal.set(uuid);

        String tid = (String)rec.get(DTRACE_IN);
        if (tid == null) {
            tid = "";
            rec.put(DTRACE_IN, tid);
        }

        tracer.newAttr(DTRACE_IN, uuid+tid);
        tidLocal.set(tid);

        return rec;
    }
}
