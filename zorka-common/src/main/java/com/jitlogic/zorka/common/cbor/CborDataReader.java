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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.cbor.CBOR.*;

/**
 *
 */
public class CborDataReader {

    private CborInput input;

    public CborDataReader(CborInput input) {
        this.input = input;
    }

    public CborDataReader(byte[] buf) {
        this.input = new ByteArrayCborInput(buf);
    }

    public int position() {
        return input.position();
    }


    public void position(int pos) {
        input.position(pos);
    }


    public int size() { return input.size(); }


    public int read() {
        return input.readI();
    }


    public long readLong() {
        int b = input.readI(), t = b & TYPE_MASK;
        long v = (b & VALU_MASK);

        switch ((int)v) {
            case UINT_CODE1:
                v = input.readL();
                break;
            case UINT_CODE2:
                v = (input.readL() << 8) | input.readL();
                break;
            case UINT_CODE4:
                v = (input.readL() << 24) |
                    (input.readL() << 16) |
                    (input.readL() << 8) |
                    input.readL();
                break;
            case UINT_CODE8:
                v = (input.readL() << 56) |
                    (input.readL() << 48) |
                    (input.readL() << 40) |
                    (input.readL() << 32) |
                    (input.readL() << 24) |
                    (input.readL() << 16) |
                    (input.readL() << 8) |
                    input.readL();
                break;
            default:
                if (v > UINT_CODE8) {
                    throw new ZorkaRuntimeException("Invalid prefix code: " + b);
                }
        }

        return t == NINT_BASE ? -v-1 : v;
    }


    public int readInt() {
        int b = input.readI();
        int v = b & VALU_MASK, t = b & TYPE_MASK;

        switch (v) {
            case UINT_CODE1:
                v = input.readI();
                break;
            case UINT_CODE2:
                v = (input.readI() << 8) | input.readI();
                break;
            case UINT_CODE4: v = (input.readI() << 24) | (input.readI() << 16) | (input.readI() << 8) | input.readI();
                break;
            case UINT_CODE8:
                throw new ZorkaRuntimeException("Expected int but encountered long at pos " + input.position());
            default:
                if (v > UINT_CODE8) {
                    throw new ZorkaRuntimeException("Invalid prefix code: " + b);
                }
        }

        return t == NINT_BASE ? -v-1 : v;
    }


    public byte[] readBytes() {
        int type = peekType();
        if (type != BYTES_BASE && type != STR_BASE) {
            throw new ZorkaRuntimeException("Expected byte array but got type=" + type);
        }
        int len = readInt();
        return input.readB(len);
    }


    public String readStr() {
        int type = peekType();
        if (type != STR_BASE) {
            throw new ZorkaRuntimeException("Expected string data but got type=" + type);
        }
        return new String(readBytes());
    }

    public int readTag() {
        int type = peekType();
        if (type != TAG_BASE) {
            throw new ZorkaRuntimeException(String.format("Expected tag, got type %02x", type));
        }
        return readInt();
    }

    public int peek() {
        return input.peekB() & 0xff;
    }


    public int peekType() {
        return input.peekB() & TYPE_MASK;
    }

    public Object readObj() {
        int peek = peek(), type = peek & TYPE_MASK;

        switch (type) {
            case UINT_BASE:
            case NINT_BASE: {
                return readLong();
            }
            case BYTES_BASE: {
                return readBytes();
            }
            case STR_BASE: {
                return readStr();
            }
            case ARR_BASE: {
                if (peek == ARR_VCODE) {
                    List<Object> lst = new ArrayList<Object>();
                    read();
                    while (peek() != BREAK_CODE) {
                        lst.add(read());
                    }
                    return lst;
                } else {
                    int len = readInt();
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
                    read();
                    while (peek() != BREAK_CODE) {
                        m.put(read(), read());
                    }
                } else {
                    int len = readInt();
                    for (int i = 0; i < len; i++) {
                        m.put(read(), read());
                    }
                }
                return m;
            }
            case TAG_BASE: {
                throw new ZorkaRuntimeException("Invalid tag for custom data: " + peek);
            }
            case SIMPLE_BASE: {
                read();
                switch (peek) {
                    case FALSE_CODE: return false;
                    case TRUE_CODE: return true;
                    case NULL_CODE: return null;
                    case UNKNOWN_CODE: return null;
                    default:
                        throw new ZorkaRuntimeException("Invalid value: " + peek);
                }
            }
        }
        throw new ZorkaRuntimeException("Type " + type + " not allowed in custom data.");
    }
}
