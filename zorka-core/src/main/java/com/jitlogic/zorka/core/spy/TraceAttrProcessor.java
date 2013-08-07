/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;

import java.util.Map;

/**
 * Adds custom attribute to current frame of collected call trace. Current method must be
 * instrumented to trace in order for this processor to take any effect.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceAttrProcessor implements SpyProcessor {

    /** Logger */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /** Tracer object */
    private Tracer tracer;

    private SymbolRegistry symbolRegistry;

    /** source field name */
    private String srcField;

    /** Attribute ID (as taken from symbol registry) */
    private int attrId;

    /**
     * Creates custom attribute processor.
     *
     * @param tracer tracer object
     *
     * @param srcField source field name
     *
     * @param traceAttr attribute ID
     */
    public TraceAttrProcessor(SymbolRegistry symbolRegistry, Tracer tracer, String srcField, String traceAttr) {
        this.tracer = tracer;
        this.srcField = srcField;
        this.symbolRegistry = symbolRegistry;
        this.attrId = symbolRegistry.symbolId(traceAttr);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        Object val = record.get(srcField);


        if (val != null) {
            if (ZorkaLogger.isLogLevel(ZorkaLogger.ZSP_ARGPROC)) {
                TraceRecord top = ((TraceBuilder)tracer.getHandler()).realTop();
                log.debug(ZorkaLogger.ZSP_ARGPROC, "Value: '" + val + "' stored as trace attribute "
                    + symbolRegistry.symbolName(attrId) + " (classId= " + top.getClassId() + " methodId=" + top.getMethodId()
                    + " signatureId=" + top.getSignatureId() + ")");
            }
            tracer.getHandler().newAttr(attrId, val);
        } else {
            if (ZorkaLogger.isLogLevel(ZorkaLogger.ZSP_ARGPROC)) {
                log.debug(ZorkaLogger.ZSP_ARGPROC, "Null value received. ");
            }
        }

        return record;
    }

}
