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

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.cbor.CBOR.*;
import static com.jitlogic.zorka.common.cbor.TraceDataTags.*;


/**
 *
 */
public class TraceDataReader implements Runnable {

    private CborDataReader reader;
    private TraceDataProcessor output;

    private boolean running = true;

    public TraceDataReader(CborDataReader reader, TraceDataProcessor output) {
        this.reader = reader;
        this.output = output;
    }

    private void checked(boolean cond, String msg) {
        if (!cond) {
            throw new ZorkaRuntimeException(msg);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running && reader.size() - reader.position() > 0) {
            process();
        }
    }

    private void process() {
        int peek = reader.read();
        int type = peek & TYPE_MASK;

        if (peek == BREAK_CODE) return;
        checked(type == TAG_BASE, "Expected tagged data.");

        int tag = peek & VALU_MASK;

        switch (tag) {
            case TAG_STRING_DEF: {
                int code = reader.read();
                checked(code == (ARR_BASE+2), "Expected 2-item tuple.");
                // [TAG=0x01](symbolId,symbol)
                output.stringRef(reader.readInt(), reader.readStr());
                break;
            }
            case TAG_METHOD_DEF: {
                int code = reader.read();
                checked(code == (ARR_BASE+4), "Expected 4-item tuple.");
                // [TAG=0x02](symbolId,classId,methodId,signatureId)
                output.methodRef(reader.readInt(), reader.readInt(), reader.readInt(), reader.readInt());
                break;
            }
            case TAG_TRACE_START: {
                int code = reader.read();
                checked(code == ARR_VCODE, "Trace record should be encoded as unbounded array.");
                int pos = reader.position();
                // [TAG=0x03](tstart,methodId)
                output.traceStart(pos - 2, reader.readLong(), reader.readInt());
                break;
            }
            case TAG_TRACE_END: {
                int code = reader.read();
                checked(code == (ARR_BASE+2) || code == (ARR_BASE+3), "Expected 2 or 3-element array.");
                long tstop = reader.readLong();
                long calls = reader.readLong();
                int flags = 0;
                if (code == ARR_BASE+3) {
                    flags = reader.readInt();
                }
                checked(reader.read() == BREAK_CODE, "Expected BREAK code.");
                output.traceEnd(reader.position(), tstop, calls, flags);
                break;
            }
            case TAG_TRACE_BEGIN: {
                int code = reader.read();
                if (code == ARR_BASE+2) {
                    // [TAG=0x05](tstamp,ttypeId)
                    output.traceBegin(reader.readLong(), reader.readInt(), 0L, 0L);
                } else if (code == ARR_BASE+3) {
                    // [TAG=0x05](tstamp,ttypeId,spanId)
                    output.traceBegin(reader.readLong(), reader.readInt(), reader.readLong(), 0L);
                } else if (code == ARR_BASE+4) {
                    // [TAG=0x05](tstamp,ttypeId,spanId,parentId)
                    output.traceBegin(reader.readLong(), reader.readInt(), reader.readLong(), reader.readLong());
                } else {
                    throw new ZorkaRuntimeException("Expected 2-, 3- or 4-element array.");
                }
                break;
            }
            case TAG_TRACE_ATTR: {
                int code = reader.read();
                checked(code == (ARR_BASE+2) || code == (ARR_BASE+3), "TraceAttr should be 2-element array");
                if (code == ARR_BASE+2) {
                    // [TAG=0x06](attrId,attrVal)
                    output.traceAttr(reader.readInt(), reader.readObj());
                } else {
                    // [TAG=0x06](ttypeId,attrId,attrVal)
                    output.traceAttr(reader.readInt(), reader.readInt(), reader.readObj());
                }
                break;
            }
            case TAG_EXCEPTION: {
                int code = reader.read();
                checked(code == (ARR_BASE+5) || code == (ARR_BASE+6),
                    "Exception description should be 5- or 6-element array.");

                long excId = reader.readLong();
                int classId = reader.readInt();
                String msg = reader.readStr();
                long cause = reader.readLong();

                int pk = reader.peek();
                checked((pk & TYPE_MASK) == ARR_BASE || pk == NULL_CODE, "Expected array or null, got peek=" + pk);
                int stackSize = pk != NULL_CODE ? reader.readInt() : 0;
                List<int[]> stackTrace = new ArrayList<int[]>(stackSize);
                for (int i = 0; i < stackSize; i++) {
                    if (reader.read() != (ARR_BASE+4)) throw new ZorkaRuntimeException("Expected 4-element tuple");
                    int[] si = new int[4];
                    si[0] = reader.readInt();
                    si[1] = reader.readInt();
                    si[2] = reader.readInt();
                    si[3] = reader.readInt();
                    stackTrace.add(si);
                }

                if (code == ARR_BASE+5) {
                    output.exception(excId, classId, msg, cause, stackTrace, null);
                } else {
                    Object attrs = reader.readObj();
                    if (!(attrs instanceof Map)) throw new ZorkaRuntimeException("Expected map.");
                    output.exception(excId, classId, msg, cause, stackTrace, (Map)attrs);
                }
                break;
            }
            case TAG_EXCEPTION_REF:
                output.exceptionRef(reader.readLong());
                break;
            default:
                throw new ZorkaRuntimeException("Invalid tag: " + tag);
        } // switch
    } // process()
}
