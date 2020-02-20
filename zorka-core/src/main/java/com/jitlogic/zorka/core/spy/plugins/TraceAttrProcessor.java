/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.ltracer.LTraceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Adds custom attribute to current frame of collected call trace. Current method must be
 * instrumented to trace in order for this processor to take any effect.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceAttrProcessor implements SpyProcessor {

    public final static int FIELD_GETTING_PROCESSOR = 1;

    public final static int STRING_FORMAT_PROCESSOR = 2;

    /**
     * Logger
     */
    private Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Tracer object
     */
    private Tracer tracer;


    /**
     * Symbol Registry.
     */
    private SymbolRegistry symbolRegistry;

    private int type;

    /**
     * Trace ID (if any).
     */
    private int traceId;

    /** Map attr id -> value/name */
    private Map<Integer,String> attrs = new TreeMap<Integer, String>();


    /**
     * @param symbolRegistry agent's symbol registry
     * @param tracer         tracer object
     * @param type           processor type (field getting or string formatting)
     * @param attrs          map: name - value/src
     * @param traceName      if processor works not on current record but on given trace
     */
    public TraceAttrProcessor(SymbolRegistry symbolRegistry, Tracer tracer, int type,
                              String traceName, Map<String,String> attrs) {
        this.tracer = tracer;
        this.symbolRegistry = symbolRegistry;
        this.type = type;

        for (Map.Entry<String,String> e : attrs.entrySet()) {
            this.attrs.put(symbolRegistry.symbolId(e.getKey()), e.getValue());
        }

        this.traceId = traceName == null ? 0 : symbolRegistry.symbolId(traceName);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        for (Map.Entry<Integer,String> e : attrs.entrySet()) {
            Object val = type == FIELD_GETTING_PROCESSOR ? record.get(e.getValue())
                : ObjectInspector.substitute(e.getValue(), record);
            if (val != null) {
                if (log.isDebugEnabled()) {
                    TraceRecord top = ((LTraceHandler) (tracer.getHandler())).realTop(); // TODO fix when STracer implementation appears
                    log.debug("Value: '" + val + "' stored as trace attribute "
                        + symbolRegistry.symbolName(e.getKey()) + " (classId= " + top.getClassId() + " methodId=" + top.getMethodId()
                        + " signatureId=" + top.getSignatureId() + ")");
                }
                tracer.getHandler().newAttr(traceId, e.getKey(), val);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Null value received. ");
                }
            }
        }

        return record;
    }

}
