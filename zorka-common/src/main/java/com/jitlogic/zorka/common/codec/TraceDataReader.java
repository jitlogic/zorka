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

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.cbor.CBOR.*;
import static com.jitlogic.zorka.cbor.TraceDataTags.*;
import static com.jitlogic.zorka.common.codec.Debug.TRACE_ENABLED;


/**
 *
 */
public class TraceDataReader implements Runnable {

    private final static Logger log = LoggerFactory.getLogger(TraceDataReader.class);

    private CborBufReader reader;
    private TraceDataProcessor output;

    private boolean running = true;

    // TODO use CborDataReader instead of implementing things by hand

    public TraceDataReader(CborBufReader reader, TraceDataProcessor output) {
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



    private Object read() {
        int peek = reader.peek(), type = peek & TYPE_MASK;

        switch (type) {
            case UINT_BASE:
            case NINT_BASE: {
                return reader.readLong();
            }
            case BYTES_BASE: {
                return reader.readBytes();
            }
            case STR_BASE: {
                return reader.readStr();
            }
            case ARR_BASE: {
                if (peek == ARR_VCODE) {
                    List<Object> lst = new ArrayList<Object>();
                    reader.read();
                    while (reader.peek() != BREAK_CODE) {
                        lst.add(read());
                    }
                    return lst;
                } else {
                    int len = reader.readInt();
                    List<Object> lst = new ArrayList<Object>(len);
                    for (int i = 0; i < len; i++) {
                        lst.add(read());
                    }
                    return lst;
                }
            }
            case MAP_BASE: {
                Map<Object,Object> m = new HashMap<Object, Object>();
                if (peek == MAP_VCODE) {
                    reader.read();
                    while (reader.peek() != BREAK_CODE) {
                        m.put(read(), read());
                    }
                } else {
                    int len = reader.readInt();
                    for (int i = 0; i < len; i++) {
                        m.put(read(), read());
                    }
                }
                return m;
            }
            case TAG_BASE: {
                switch (peek) {
                    case TAG_BASE+TAG_STRING_REF:
                        reader.readInt();
                        return new ObjectRef(reader.readInt());
                    default:
                        throw new ZorkaRuntimeException("Invalid tag for custom data: " + peek);

                }
            }
            case SIMPLE_BASE: {
                switch (peek) {
                    case FALSE_CODE:
                        reader.read();
                        return false;
                    case TRUE_CODE:
                        reader.read();
                        return true;
                    case NULL_CODE:
                        reader.read();
                        return null;
                    case UNKNOWN_CODE:
                        reader.read();
                        return null;
                    default:
                        throw new ZorkaRuntimeException("Invalid value: " + peek);
                }
            }
        }
        throw new ZorkaRuntimeException("Type " + type + " not allowed in custom data.");
    }


    private void process() {
        int peek = reader.peek();
        int type = peek & TYPE_MASK;

        if (type != TAG_BASE) {
            if (peek == BREAK_CODE) {
                reader.read();
                output.traceEnd(0, 0); // TODO this is ambiguous, we should really have some distinct marker for trace end
                return;
            } else {
                throw new ZorkaRuntimeException("Invalid datatype (expected tagged data): " + type);
            }
        }

        int tag = reader.readInt();
        if (TRACE_ENABLED) Debug.trace("TraceDataReader","process(): TAG=" + tag);
        switch (tag) {
            case TAG_TRACE_START: {
                checked(reader.peek() == ARR_VCODE,
                    "Trace record should be encoded as unbounded array.");
                int pos = reader.position();
                reader.read();
                tag = reader.readTag();
                checked(tag == TAG_PROLOG_BE || tag == TAG_PROLOG_LE, "Expected trace prolog.");
                checked(reader.peek() == (BYTES_BASE + 8), "Invalid trace record prolog.|");
                reader.read();
                long v = reader.readRawLong(tag == TAG_PROLOG_LE);
                long tstart = v & 0x000000FFFFFFFFFFL;
                int methodId = (int) (v >>> 40);
                output.traceStart(pos - 1, tstart, methodId);
                break;
            }
            case TAG_EPILOG_BE:
            case TAG_EPILOG_LE: {
                int xf = reader.peek();
                checked(xf == (BYTES_BASE + 8) || xf == (BYTES_BASE + 16), "Invalid trace epilog.");
                reader.read();
                long v = reader.readRawLong(tag == TAG_EPILOG_LE);
                long tstop = v & 0x000000FFFFFFFFFFL;
                if (xf == 0x48) {
                    int calls = (int) (v >>> 40);
                    output.traceEnd(tstop, calls);
                } else {
                    long calls = reader.readRawLong(tag == TAG_EPILOG_LE);
                    output.traceEnd(tstop, calls);
                }
                if (reader.peek() != BREAK_CODE) {
                    throw new ZorkaRuntimeException("Epilog should be last element of trace record.");
                }
                reader.read();
                break;
            }
            case TAG_TRACE_ATTR: {
                Object attrs = read();
                output.traceAttr((Map)attrs);
                break;
            }
            case TAG_TRACE_BEGIN: {
                int prefix = reader.peek();
                checked(prefix >= 0x81 && prefix <= 0x83,
                    "Trace begin marker should be 2-element array.");
                reader.read();
                // Timestamp (mandatory)
                long tstamp = reader.readLong();
                long spanId = 0, parentId = 0;

                // SpanID (optional)
                if (prefix >= 0x82) {
                    spanId = reader.readLong();
                }
                // ParentID (optional)
                if (prefix >= 0x83) {
                    parentId = reader.readLong();
                }
                output.traceBegin(tstamp, spanId, parentId);
                break;
            }
            case TAG_EXCEPTION: {
                // TODO handle also stored exception format (with refs), not only wire format
                checked(reader.peek() == 0x85,
                    "Exception description should be 5-element array.");
                reader.read();
                int excId = reader.readInt();
                checked(reader.peek() == TAG_BASE+TAG_STRING_REF,
                    "Expected exception class (as string ref).");
                reader.read();
                int excClass = reader.readInt();
                String msg = null;
                int msgId = 0;

                if (reader.peekType() == STR_BASE) {
                    msg = reader.readStr();  // TODO handle refs here
                } else if (reader.peek() == NULL_CODE) {
                    msg = "";
                    reader.read();
                } else {
                    msgId = reader.readInt();
                }

                int causeId = reader.readInt();

                ExceptionData ex = msg != null
                    ? new ExceptionData(excId, excClass, msg, causeId)
                    : new ExceptionData(excId, excClass, msgId, causeId);

                checked(ARR_BASE == reader.peekType(),
                    "Expected stack trace (sized array).");
                int stackSize = reader.readInt();
                for (int i = 0; i < stackSize; i++) {
                    checked(reader.peek() == ARR_BASE + 4,
                        "Expected stack trace element (4-element item).");
                    reader.read();
                    int sClass = reader.readInt();
                    int sMethod = reader.readInt();
                    int sFile = reader.readInt();
                    int sLine = reader.readInt();
                    ex.stackTrace.add(new StackData(sClass, sMethod, sFile, sLine));
                }
                output.exception(ex);
                break;
            }
            case TAG_EXCEPTION_REF:
                output.exceptionRef(reader.readInt());
                break;
            case TAG_TRACE_FLAGS:
                output.traceFlags(reader.readInt());
                break;
            default:
                throw new ZorkaRuntimeException("Invalid tag: " + tag);
        } // switch

    } // process()


}
