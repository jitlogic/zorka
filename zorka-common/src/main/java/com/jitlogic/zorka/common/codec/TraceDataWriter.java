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

package com.jitlogic.zorka.common.codec;

import com.jitlogic.zorka.cbor.CborDataWriter;

import java.util.Map;

import static com.jitlogic.zorka.cbor.CBOR.*;
import static com.jitlogic.zorka.cbor.TraceDataTags.*;

/**
 *
 */
public class TraceDataWriter implements TraceDataProcessor {

    private CborDataWriter writer;

    public TraceDataWriter(CborDataWriter writer) {
        this.writer = writer;
    }


    @Override
    public void traceStart(int pos, long tstart, int methodId) {
        writer.writeTag(TAG_TRACE_START);
        writer.write(ARR_VCODE);
        writer.writeTag(TAG_PROLOG_LE);
        writer.write(BYTES_BASE + 8);
        writer.writeRawLong((((long)methodId) << 40) | tstart, true);
    }


    @Override
    public void traceEnd(long tstop, long calls) {
        writer.writeTag(TAG_EPILOG_LE);
        if (calls > 0xFFFFFFL) {
            writer.write(BYTES_BASE+16);
            writer.writeRawLong(tstop, true);
            writer.writeRawLong(calls, true);
        } else {
            writer.write(BYTES_BASE+8);
            writer.writeRawLong((calls << 40) | tstop, true);
        }
        writer.write(BREAK_CODE);
    }

    @Override
    public void traceBegin(long tstamp, long spanId, long parentId) {
        int pfx = ARR_BASE+1;
        if (spanId != 0) pfx++;
        if (parentId != 0) pfx++;

        writer.writeTag(TAG_TRACE_BEGIN);
        writer.write(pfx);
        writer.writeLong(tstamp);

        if (spanId != 0) writer.writeLong(spanId);
        if (parentId != 0) writer.writeLong(parentId);
    }

    @Override
    public void traceFlags(int flags) {
        writer.writeTag(TAG_TRACE_FLAGS);
        writer.writeInt(flags);
    }


    @Override
    public void traceAttr(Map<Object,Object> data) {
        writer.writeTag(TAG_TRACE_ATTR);
        writer.writeUInt(MAP_BASE, data.size());
        for (Map.Entry<Object,Object> e : data.entrySet()) {
            writer.writeObj(e.getKey());
            writer.writeObj(e.getValue());
        }
    }


    @Override
    public void exceptionRef(int ref) {
        writer.writeTag(TAG_EXCEPTION_REF);
        writer.writeInt(ref);
    }


    @Override
    public void exception(ExceptionData ex) {
        writer.writeTag(TAG_EXCEPTION);
        writer.writeUInt(ARR_BASE, 5);

        writer.writeInt(ex.id);

        if (ex.msg != null ) {
            writer.writeString(ex.msg);
        } else {
            writer.writeInt(ex.msgId);
        }

        writer.writeUInt(ARR_BASE, ex.stackTrace.size());
        for (StackData se : ex.stackTrace) {
            writer.writeUInt(ARR_BASE, 4);
            writer.writeInt(se.classId);
            writer.writeInt(se.methodId);
            writer.writeInt(se.fileId);
            writer.writeInt(se.lineNum);
        }

        writer.writeInt(ex.causeId);
    }

    @Override
    public void commit() {

    }
}
