/*
 * Copyright (c) 2012-2020 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.common.cbor;



import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.common.cbor.CBOR.*;

public class CborDataWriter {

    protected boolean strict = true;
    protected byte[] buf;
    protected int pos, limit, delta;

    public CborDataWriter(int buflen, int delta) {
        this(buflen, delta, true);
    }

    public CborDataWriter(int buflen, int delta, boolean strict) {
        this.buf = new byte[buflen];
        this.pos = 0;
        this.limit = buf.length;
        this.delta = delta;
        this.strict = strict;
    }

    public CborDataWriter(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public CborDataWriter(byte[] buf, int offs, int len) {
        this.buf = buf;
        this.pos = offs;
        this.limit = offs + len;
    }


    public void write(int b) {
        if (pos == limit) {
            flush();
        }
        buf[pos++] = (byte)b;
    }


    public void write(byte[] b) {
        write(b, 0, b.length);
    }


    public void write(byte[] b, int offs, int len) {
        while (len > 0) {
            if (pos+len >= limit) {
                flush();
            }
            int l = Math.min(len, limit-pos);
            System.arraycopy(b, offs, buf, pos, l);
            len -= l; pos += l; offs += l;
        }
    }


    public void ensure(int l) {
        while (limit - pos < l) {
            flush();
        }
    }


    public void flush() {
        if (delta > 0) {
            byte[] b = new byte[limit+delta];
            System.arraycopy(this.buf, 0, b, 0, limit);
            this.buf = b;
            limit += delta;
        } else {
            throw new RuntimeException("Buffer overflow.");
        }
    }


    public void write(int prefix, int i) {
        ensure(5);
        if (i < 0x18) {
            write(prefix + i);
        } else if (i < 0x100) {
            write(prefix + 0x18);
            write(i & 0xff);
        } else if (i < 0x10000) {
            write(prefix + 0x19);
            write((byte) ((i >> 8) & 0xff));
            write((byte) (i & 0xff));
        } else {
            byte[] b = new byte[5];
            b[0] = (byte) (0x1a + prefix);
            b[1] = (byte) ((i >> 24) & 0xff);
            b[2] = (byte) ((i >> 16) & 0xff);
            b[3] = (byte) ((i >> 8) & 0xff);
            b[4] = (byte) (i & 0xff);
            write(b);
        }
    }


    public void write(int prefix, long l) {
        if (l < Integer.MAX_VALUE) {
            write(prefix, (int)l);
        } else {
            ensure(9);
            byte[] b = new byte[9];
            b[0] = (byte) (0x1b + prefix);
            b[1] = (byte) ((l >> 56) & 0xff);
            b[2] = (byte) ((l >> 48) & 0xff);
            b[3] = (byte) ((l >> 40) & 0xff);
            b[4] = (byte) ((l >> 32) & 0xff);
            b[5] = (byte) ((l >> 24) & 0xff);
            b[6] = (byte) ((l >> 16) & 0xff);
            b[7] = (byte) ((l >> 8) & 0xff);
            b[8] = (byte)  (l & 0xff);
            write(b);
        }
    }


    public void writeInt(int i) {
        if (i >= 0) {
            write(0, i);
        } else {
            write(0x20, Math.abs(i)-1);
        }
    }


    public void writeLong(long l) {
        if (l >= 0) {
            write(0, l);
        } else {
            write(CBOR.NINT_BASE, Math.abs(l)-1L);
        }
    }


    public void writeBytes(byte[] b) {
        write(CBOR.ARR_BASE, b.length);
        write(b);
    }


    public void writeString(String s) {
        if (s != null) {
            byte[] b = s.getBytes();
            write(CBOR.STR_BASE, b.length);
            write(b);
        } else {
            writeNull();
        }
    }


    public void writeTag(int tag) {
        write(CBOR.TAG_BASE, tag);
    }


    public void writeSimpleToken(int token) {
        write(CBOR.SIMPLE_BASE, token);
    }


    public void writeObj(Object obj) {
        if (obj == null) {
            write(CBOR.NULL_CODE);
        } else if (obj.getClass() == Integer.class) {
            writeInt((Integer) obj);
        } else if (obj.getClass() == Long.class) {
            writeLong((Long) obj);
        } else if (obj.getClass() == String.class) {
            writeString((String) obj);
        } else if (obj instanceof List) {
            List lst = (List)obj;
            write(CBOR.ARR_BASE, lst.size());
            for (Object o : lst) {
                writeObj(o);
            }
        } else if (obj instanceof Map) {
            Map<Object,Object> m = (Map<Object,Object>)obj;
            write(CBOR.MAP_BASE, m.size());
            for (Map.Entry<Object,Object> e : m.entrySet()) {
                writeObj(e.getKey());
                writeObj(e.getValue());
            }
        } else if (obj.getClass() == Boolean.class) {
            write(Boolean.TRUE.equals(obj) ? TRUE_CODE : FALSE_CODE);
        } else if (obj.getClass() == Short.class || obj.getClass() == Byte.class) {
            writeInt(((Number)obj).intValue());
        } else if (obj.getClass() == Float.class || obj.getClass() == Double.class || !strict) {
            writeString(""+obj); // TODO handle float values properly
        } else {
            throw new RuntimeException("Unsupported data type: " + obj.getClass());
        }
    }

    public void writeNull() {
        write(CBOR.NULL_CODE);
    }

    public void position(int pos) {
        this.pos = pos;
    }

    public int position() {
        return this.pos;
    }

    public void reset() {
        pos = 0;
    }

    public byte[] getBuf() {
        return buf;
    }

    public byte[] toByteArray() {
        if (pos == 0) return new byte[0];
        byte[] rslt = new byte[pos];
        System.arraycopy(buf, 0, rslt, 0, pos);
        return rslt;
    }

    public byte[] toByteArray(int offs, int len) {
        if (offs+len > pos) throw new ZorkaRuntimeException("Tried to read outside of output buffer.");
        byte[] rslt = new byte[len];
        System.arraycopy(buf, offs, rslt, 0, len);
        return rslt;
    }
}
