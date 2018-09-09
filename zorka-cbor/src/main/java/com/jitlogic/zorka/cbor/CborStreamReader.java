/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.cbor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.cbor.CBOR.*;

public class CborStreamReader {

    private InputStream in;

    private TagProcessor tagProcessor;
    private SimpleValResolver simpleValResolver;

    byte[] buf = new byte[16];

    public CborStreamReader(InputStream in) throws IOException {
        this.in = in;
    }

    public CborStreamReader(InputStream in, TagProcessor tagProcessor, SimpleValResolver simpleValResolver) {
        this.in = in;
        this.tagProcessor = tagProcessor;
        this.simpleValResolver = simpleValResolver;
    }


    private int readBytes(byte[] b, int offs, int len) throws IOException {
        int done = 0;
        do {
            int i = in.read(b, offs+done,len-done);
            if (i == -1) {
                return done > 0 ? done : -1;
            }
            done += i;
        } while (len-done > 0);
        return done;
    }


    private int readUInt(int b0) throws IOException {
        int i = b0 & UINT_MASK;
        if (i < UINT_CODE1) {
            return i;
        } else if (i == UINT_CODE1) {
            return in.read();
        } else if (i == UINT_CODE2) {
            return ((in.read() << 8) | in.read());
        } else if (i == UINT_CODE4) {
            readBytes(buf, 0, 4);
            return (((int)buf[0] & 0xff) << 24)
                |  (((int)buf[1] & 0xff) << 16)
                |  (((int)buf[2] & 0xff) << 8)
                |   ((int)buf[3] & 0xff);
        }
        throw new IOException(String.format("Invalid prefix: 0x%x", b0));
    }


    private long readULong(int b0) throws IOException {
        int i = b0 & UINT_MASK;
        if (i == UINT_CODE8) {
            readBytes(buf, 0, 8);
            return (((long)buf[0] & 0xff) << 56)
                |  (((long)buf[1] & 0xff) << 48)
                |  (((long)buf[2] & 0xff) << 40)
                |  (((long)buf[3] & 0xff) << 32)
                |  (((long)buf[4] & 0xff) << 24)
                |  (((long)buf[5] & 0xff) << 16)
                |  (((long)buf[6] & 0xff) << 8)
                |   ((long)buf[7] & 0xff);
        } else {
            return readUInt(b0);
        }
    }


    public Object read() throws IOException {
        int i = in.read();
        if (i == -1) {
            return BREAK;
        } else if (i < UINT_CODE8) {
            return readUInt(i);
        } else if (i == UINT_CODE8) {
            return readULong(i);
        } else if (i < NINT_CODE8) {
            return -1 * readUInt(i) - 1;
        } else if (i == NINT_CODE8) {
            return -1L * readULong(i) - 1L;
        } else if (i < BYTES_CODE8) {
            int len = readUInt(i);
            byte[] b = new byte[len];
            readBytes(b, 0, b.length);
            return b;
        } else if (i == BYTES_VCODE) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (Object o = read(); o != BREAK; o = read()) {
                if (o instanceof byte[]) {
                    bos.write((byte[]) o);
                } else {
                    throw new IOException("Expected byte array but got " + (o == null ? "null" : o.getClass()));
                }
            }
        } else if (i < STR_CODE8) {
            int len = readUInt(i);
            byte[] b = new byte[len];
            readBytes(b, 0, b.length);
            return new String(b);
        } else if (i == STR_VCODE) {
            StringBuilder sb = new StringBuilder();
            for (Object o = read(); o != BREAK; o = read()) {
                if (o == null || o.getClass() != String.class) {
                    throw new IOException("Expected string but got " + (o == null ? "null" : o.getClass()));
                }
                sb.append(o);
            }
            return sb.toString();
        } else if (i < ARR_CODE8) {
            int len = readUInt(i);
            List<Object> lst = new ArrayList<Object>(len);
            for (int x = 0; x < len; x++) {
                lst.add(read());
            }
            return lst;
        } else if (i == ARR_VCODE) {
            List<Object> lst = new ArrayList<Object>();
            for (Object o = read(); o != BREAK; o = read()) {
                lst.add(o);
            }
            return lst;
        } else if (i < MAP_CODE8) {
            int size = readUInt(i);
            Map<Object,Object> map = new HashMap<Object,Object>();
            for (int x = 0; x < size; x++) {
                map.put(read(), read());
            }
            return map;
        } else if (i == MAP_VCODE) {
            Map<Object, Object> m = new HashMap<Object, Object>();
            for (Object k = read(); k != BREAK; k = read()) {
                Object v = read();
                if (v == BREAK) {
                    throw new IOException("Unexpected BREAK code when reading unbounded map.");
                }
                m.put(k, v);
            }
            return m;
        } else if (i < TAG_CODE8) {
            int tag = readUInt(i);
            Object obj = read();
            return tagProcessor != null ? tagProcessor.process(tag, obj) : obj;
        } else if (i == NULL_CODE) {
            return null;
        } else if (i == TRUE_CODE) {
            return true;
        } else if (i == FALSE_CODE) {
            return false;
        } else if (i == UNKNOWN_CODE) {
            return UNKNOWN;
        } else if (i < SIMPLE_END) {
            return simpleValResolver.resolve(readUInt(i));
        } else if (i == FLOAT_BASE2) {
            return fromHalfFloat(((in.read() & 0xff) << 8) | ((in.read() & 0xff)));
        } else if (i == FLOAT_BASE4) {
            readBytes(buf, 0, 4);
            return Float.intBitsToFloat(
                (((int) buf[0] & 0xff) << 24)
                    | (((int) buf[1] & 0xff) << 16)
                    | (((int) buf[2] & 0xff) << 8)
                    | (((int) buf[3] & 0xff)));
        } else if (i == FLOAT_BASE8) {
            readBytes(buf, 0, 8);
            return Double.longBitsToDouble(
                      (((long) buf[0] & 0xff) << 56)
                    | (((long) buf[1] & 0xff) << 48)
                    | (((long) buf[2] & 0xff) << 40)
                    | (((long) buf[3] & 0xff) << 32)
                    | (((long) buf[4] & 0xff) << 24)
                    | (((long) buf[5] & 0xff) << 16)
                    | (((long) buf[6] & 0xff) << 8)
                    | (((long) buf[7] & 0xff)));
        } else if (i == BREAK_CODE) {
            return BREAK;
        }
        throw new IOException("Invalid prefix: " + i);
    }


    public static float fromHalfFloat(int h) {
        switch (h) {
            case 0x0000: return 0.0f;
            case 0x7c00: return Float.POSITIVE_INFINITY;
            case 0x7e00: return Float.NaN;
            case 0x8000: return -0.0f;
            case 0xfc00: return Float.NEGATIVE_INFINITY;
            default:
                return Float.intBitsToFloat(((h & 0x8000)<<16) | (((h & 0x7c00)+0x1C000)<<13) | ((h & 0x03FF)<<13));
        }
    }

}


