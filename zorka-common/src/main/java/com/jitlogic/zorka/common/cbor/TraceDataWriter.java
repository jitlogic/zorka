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

import static com.jitlogic.zorka.common.cbor.CBOR.*;
import static com.jitlogic.zorka.common.cbor.TraceDataTags.*;

/**
 *
 */
public class TraceDataWriter implements TraceDataProcessor {

    private CborDataWriter writer;

    public TraceDataWriter(CborDataWriter writer) {
        this.writer = writer;
    }


    @Override
    public void stringRef(int symbolId, String symbol) {
        writer.writeTag(TAG_STRING_DEF);
        writer.write(ARR_BASE, 2);
        writer.writeInt(symbolId);
        writer.writeString(symbol);
    }

    @Override
    public void methodRef(int symbolId, int classId, int methodId, int signatureId) {
        writer.writeTag(TAG_METHOD_DEF);
        writer.write(ARR_BASE, 4);
        writer.writeInt(symbolId);
        writer.writeInt(classId);
        writer.writeInt(methodId);
        writer.writeInt(signatureId);
    }

    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        writer.writeTag(TAG_TRACE_START);
        writer.write(ARR_VCODE);
        writer.writeLong(tstart);
        writer.writeInt(methodId);
    }


    @Override
    public void traceEnd(long tstop, long calls, int flags) {
        writer.writeTag(TAG_TRACE_END);
        writer.write(ARR_BASE, flags != 0 ? 3 : 2);
        writer.writeLong(tstop);
        writer.writeLong(calls);
        if (flags != 0) writer.writeInt(flags);
        writer.write(BREAK_CODE);
    }


    @Override
    public void traceAttr(int attrId, Object attrVal) {
        writer.writeTag(TAG_TRACE_ATTR);
        writer.write(ARR_BASE, 2);
        writer.writeInt(attrId);
        writer.writeObj(attrVal);
    }


    @Override
    public void traceAttr(int ttypeId, int attrId, Object attrVal) {
        writer.writeTag(TAG_TRACE_ATTR);
        writer.write(ARR_BASE, 3);
        writer.writeInt(ttypeId);
        writer.writeInt(attrId);
        writer.writeObj(attrVal);
    }


    @Override
    public void traceBegin(long tstamp, int traceId, long spanId, long parentId) {
        int pfx = ARR_BASE+2;
        if (spanId != 0) pfx++;
        if (parentId != 0) pfx++;

        writer.writeTag(TAG_TRACE_BEGIN);
        writer.write(pfx);
        writer.writeLong(tstamp);
        writer.writeLong(traceId);
        if (spanId != 0) writer.writeLong(spanId);
        if (parentId != 0) writer.writeLong(parentId);
    }


    @Override
    public void exception(long excId, int classId, String message, long causeId, List<int[]> stackTrace, Map<Integer, Object> attrs) {
        writer.writeTag(TAG_EXCEPTION);
        writer.write(ARR_BASE, attrs == null ? 5 : 6);
        writer.writeLong(excId);
        writer.writeInt(classId);
        writer.writeString(message);
        writer.writeLong(causeId);
        writer.write(ARR_BASE, stackTrace.size());
        for (int[] si : stackTrace) {
            writer.write(ARR_BASE, 4);
            writer.writeInt(si[0]); // classId
            writer.writeInt(si[1]); // methodId
            writer.writeInt(si[2]); // fileId
            writer.writeInt(si[3]); // lineNum
        }
        if (attrs != null) writer.writeObj(attrs);
    }


    @Override
    public void exceptionRef(long excId) {
        writer.writeTag(TAG_EXCEPTION_REF);
        writer.writeLong(excId);
    }

}
