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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TaggedValue;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.spy.Tracer;

import java.util.Map;

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
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

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
     * source field name
     */
    private String srcVal;

    /**
     * Trace ID (if any).
     */
    private int traceId;

    /**
     * Attribute ID (as taken from symbol registry)
     */
    private int attrId;


    /**
     * Attribute Tag ID (null if attribute should not be tagged)
     */
    private Integer attrTagId;


    /**
     * Creates custom attribute processor.
     *
     * @param symbolRegistry agent's symbol registry
     * @param tracer         tracer object
     * @param type           processor type (field getting or string formatting)
     * @param srcVal         source field name
     * @param attrName       attribute ID
     * @param attrTag        (optional) attribute tag
     */
    public TraceAttrProcessor(SymbolRegistry symbolRegistry, Tracer tracer, int type,
                              String srcVal, String attrName, String attrTag) {
        this.tracer = tracer;
        this.srcVal = srcVal;
        this.symbolRegistry = symbolRegistry;
        this.type = type;
        this.traceId = -1;
        this.attrId = symbolRegistry.symbolId(attrName);
        this.attrTagId = attrTag != null ? symbolRegistry.symbolId(attrTag) : null;
    }

    /**
     * @param symbolRegistry agent's symbol registry
     * @param tracer         tracer object
     * @param type           processor type (field getting or string formatting)
     * @param srcVal         source field name
     * @param traceName
     * @param attrName       attribute ID
     * @param attrTag        (optional) attribute tag
     */
    public TraceAttrProcessor(SymbolRegistry symbolRegistry, Tracer tracer, int type,
                              String srcVal, String traceName, String attrName, String attrTag) {
        this(symbolRegistry, tracer, type, srcVal, attrName, attrTag);
        this.traceId = traceName == null ? 0 : symbolRegistry.symbolId(traceName);
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {

        Object val = type == FIELD_GETTING_PROCESSOR ? record.get(srcVal)
                : ObjectInspector.substitute(srcVal, record);

        if (val != null) {
            if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                TraceRecord top = tracer.getHandler().realTop();
                log.debug(ZorkaLogger.ZSP_ARGPROC, "Value: '" + val + "' stored as trace attribute "
                        + symbolRegistry.symbolName(attrId) + " (classId= " + top.getClassId() + " methodId=" + top.getMethodId()
                        + " signatureId=" + top.getSignatureId() + ")");
            }
            tracer.getHandler().newAttr(traceId, attrId, attrTagId != null ? new TaggedValue(attrTagId, val) : val);
        } else {
            if (ZorkaLogger.isLogMask(ZorkaLogger.ZSP_ARGPROC)) {
                log.debug(ZorkaLogger.ZSP_ARGPROC, "Null value received. ");
            }
        }

        return record;
    }

}
