/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TraceFilterProcessor implements SpyProcessor {

    private static final ZorkaLog log = ZorkaLogger.getLog(TraceFilterProcessor.class);

    public static final Object ANY = new Object();

    private Tracer tracer;

    private String srcField;

    private Boolean defaultVal = null;

    private Set<Object> yes = new HashSet<Object>(), no = new HashSet<Object>(), maybe = new HashSet<Object>();


    public TraceFilterProcessor(Tracer tracer, String srcField, Boolean defaultVal, Set<Object> yes, Set<Object> no, Set<Object> maybe) {
        this.srcField = srcField;
        this.defaultVal = defaultVal;
        this.yes = yes != null ? yes : this.yes;
        this.no = no != null ? no : this.no;
        this.maybe = maybe != null ? maybe : this.maybe;
        this.tracer = tracer;
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = record.get(srcField);

        Boolean rslt = decide(val);

        if (rslt != null) {
            tracer.getHandler().markTraceFlags(0, rslt ? TraceMarker.SUBMIT_TRACE : TraceMarker.DROP_TRACE);
        }

        return record;
    }


    public Boolean decide(Object val) {
        Boolean rslt = yes.contains(val) ? Boolean.TRUE : (no.contains(val) ? Boolean.FALSE : null);

        if (rslt == null && !maybe.contains(val)) {
            rslt = defaultVal;
        }
        return rslt;
    }
}
