/*
 * Copyright 2016-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.cbor;

import java.util.List;
import java.util.Map;

/**
 * Interface for handling low level incoming trace data. This allows implementing stateless processors suitable for
 * transforming trace data with minimal memory footprint, yet some operations are cumbersome (eg. reconstruction, filtering).
 */
public interface TraceDataProcessor {

    /**
     * String symbol registration.
     *
     * Wire format: [TAG=0x01](symbolId,symbol)
     *
     * @param symbolId - symbol ID (agent side)
     * @param symbol - symbol string
     */
    void stringRef(int symbolId, String symbol);

    /**
     * Method symbol registration.
     *
     * Wire format: [TAG=0x02](symbolId,classId,methodId,signatureId)
     *
     * @param symbolId - symbol ID (agent side)
     * @param classId - class name (symbol ref)
     * @param methodId - method name (symbol ref)
     * @param signatureId - signature string (symbol ref)
     */
    void methodRef(int symbolId, int classId, int methodId, int signatureId);

    /**
     * Wire format: [TAG=0x03](tstart,methodId)
     *
     * @param pos - begin position (in data stream, before tag);
     * @param tstart - start time (System.nanoTime());
     * @param methodId - method ID (method symbol ref);
     */
    void traceStart(int pos, long tstart, int methodId);

    /**
     * Wire format: [TAG=0x04](tstart,calls,flags)
     *
     * @param pos - end position (in data stream, after tag)
     * @param tstop - start time (System.nanoTime());
     * @param calls - method ID;
     * @param flags (optional) - trace flag bits: 0x01 - error,
     */
    void traceEnd(int pos, long tstop, long calls, int flags);

    /**
     * Wire format: [TAG=0x05](tstamp,ttypeId,spanId,parentId)
     * @param tstamp - timestamp (System.currentTimeMillis())
     * @param ttypeId - trace type ID (symbol ref)
     * @param spanId (optional) - span ID;
     * @param parentId (optional) - parent span ID;
     */
    void traceBegin(long tstamp, int ttypeId, long spanId, long parentId);

    /**
     * Wire format: [TAG=0x06](attrId,attrVal)
     * @param attrId - attribute name (symbol ref);
     * @param attrVal - attribute value (preferably string, no refs, no tagged data);
     */
    void traceAttr(int attrId, Object attrVal);

    /**
     * Wire format: [TAG=0x06](ttypeId,attrId,attrVal)
     * @param ttypeId - trace type ID (or 0 if looking for any trace);
     * @param attrId - attribute name (symbol ref);
     * @param attrVal - attribute value (preferably string, no refs, no tagged data);
     */
    void traceAttr(int ttypeId, int attrId, Object attrVal);

    /**
     * Wire format: [TAG=0x07](excId,className,message,cause,stackTrace,attrs)
     * @param excId - exception ID (object ID in JVM);
     * @param classId - class name (symbol ref);
     * @param message - exception message;
     * @param cause - exception cause;
     * @param stackTrace - stack trace (array of tuples [classId,methodId,fileId,lineNum]);
     * @param attrs (optional) - additional attributes (optional, map attrId->attrVal);
     */
    void exception(long excId, int classId, String message, long cause,
                   List<int[]> stackTrace, Map<Integer,Object> attrs);

    /**
     * Wire format: [TAG=0x08]excId
     * @param excId - exception ID (refers to already sent exception);
     */
    void exceptionRef(long excId);

}
