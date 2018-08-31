/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy.st;

import com.jitlogic.zorka.common.util.CBOR;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;

import java.util.List;
import java.util.Map;

public abstract class STraceBuffer extends TraceHandler {

    protected STraceBufManager bufManager;
    protected STraceBufChunk chunk = null;
    /** Currently selected output buffer. */
    protected byte[] buffer;
    /** Buffer position and buffer length. */
    protected int bufOffs;
    protected int bufPos;
    protected int bufLen;

    protected int nchunks = 0;

    protected int minChunks = 2, maxChunks = 16;

    /** Trace buffer output */
    protected STraceBufOutput output;


    public STraceBuffer(STraceBufManager bufManager) {
        this.bufManager = bufManager;
    }


    protected void dropTrace() {
        if (chunk != null) {
            if (chunk.getNext() != null) {
                bufManager.put(chunk.getNext());
                chunk.setNext(null);
            }
            buffer = chunk.getBuffer();
            bufPos = 0;
            bufOffs = 0;
            bufLen = buffer.length;
            nchunks = 1;
        } else {
            buffer = null;
            bufPos = 0;
            bufOffs = 0;
            bufLen = 0;
            nchunks = 0;
        }
    }


    public void flush() {
        if (buffer != null) {
            flushChunk();
        }
        output.process(this, chunk);
        chunk = null;
    }


    protected void flushChunk() {
        if (buffer != null) {
            chunk.setSize(bufPos);
            buffer = null;
            bufLen = 0;
            bufOffs += bufPos;
            bufPos = 0;
        }
    }


    protected void nextChunk() {
        if (buffer != null) {
            flushChunk();
        }
        STraceBufChunk ch = bufManager.get();
        ch.setOffset(bufOffs);
        ch.setNext(chunk);
        chunk = ch;
        buffer = ch.getBuffer();
        bufLen = buffer.length;
    }


    public void write(int b) {
        if (bufLen - bufPos < 1) nextChunk();
        buffer[bufPos++] = (byte)b;
    }

    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) {
        int bfree = bufLen - bufPos;

        if (bfree == 0) nextChunk();

        int sz = bfree < len ? bfree : len;

        System.arraycopy(b, off, buffer, bufPos, sz);
        bufPos += sz;

        if (sz < len) {
            write(b, off+sz, len-sz);
        }
    }

    public void writeUInt(int base, int i) {
        if (i < CBOR.UINT_CODE1) {
            if (bufLen - bufPos < 1) nextChunk();
            buffer[bufPos++] = (byte)(base+i);
        } else if (i < 0x100) {
            if (bufLen - bufPos < 2) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE1);
            buffer[bufPos+1] = (byte) (i & 0xff);
            bufPos += 2;
        } else if (i < 0x10000) {
            if (bufLen - bufPos < 3) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE2);
            buffer[bufPos+1] = (byte) ((i >> 8) & 0xff);
            buffer[bufPos+2] = (byte) (i & 0xff);
            bufPos += 3;
        } else {
            if (bufLen - bufPos < 5) nextChunk();
            buffer[bufPos]   = (byte) (base+CBOR.UINT_CODE4);
            buffer[bufPos+1] = (byte) ((i >> 24) & 0xff);
            buffer[bufPos+2] = (byte) ((i >> 16) & 0xff);
            buffer[bufPos+3] = (byte) ((i >> 8) & 0xff);
            buffer[bufPos+4] = (byte) (i & 0xff);
            bufPos += 5;
        }
    }

    public void writeULong(int base, long l) {
        if (l <= Integer.MAX_VALUE) {
            writeUInt(base, (int)l);
        } else {
            if (bufLen - bufPos < 9) nextChunk();
            buffer[bufPos] = (byte) (base+CBOR.UINT_CODE8);
            buffer[bufPos+1] = (byte) ((l >> 56) & 0xff);
            buffer[bufPos+2] = (byte) ((l >> 48) & 0xff);
            buffer[bufPos+3] = (byte) ((l >> 40) & 0xff);
            buffer[bufPos+4] = (byte) ((l >> 32) & 0xff);
            buffer[bufPos+5] = (byte) ((l >> 24) & 0xff);
            buffer[bufPos+6] = (byte) ((l >> 16) & 0xff);
            buffer[bufPos+7] = (byte) ((l >> 8) & 0xff);
            buffer[bufPos+8] = (byte)  (l & 0xff);
            bufPos += 9;
        }
    }

    public void writeString(String s)  {
        byte[] b = s.getBytes();
        writeUInt(0x60, b.length);
        write(b);
    }

    public void writeList(List lst) {
        // TODO obsłużyć również array of objects, array of integers itd.
        writeUInt(0x80, lst.size());
        for (Object itm : lst) {
            writeObject(itm);
        }
    }

    public void writeMap(Map<Object,Object> map) {
        writeUInt(0xa0, map.size());
        for (Map.Entry e : map.entrySet()) {
            writeObject(e.getKey());
            writeObject(e.getValue());
        }
    }

    public void writeInt(int i) {
        if (i >= 0) {
            writeUInt(0, i);
        } else {
            writeUInt(0x20, Math.abs(i)-1);
        }
    }

    public void writeLong(long l) {
        if (l >= 0) {
            writeULong(0, l);
        } else {
            writeULong(0x20, Math.abs(l)-1L);
        }
    }

    public void writeFloat(float f) {
        int i = Float.floatToIntBits(f);
        if (bufLen - bufPos < 5) nextChunk();
        buffer[bufPos] = (byte)CBOR.FLOAT_BASE4;
        buffer[bufPos+1] = (byte)((i >> 24) & 0xff);
        buffer[bufPos+2] = (byte)((i >> 16) & 0xff);
        buffer[bufPos+3] = (byte)((i >> 8) & 0xff);
        buffer[bufPos+4] = (byte)(i & 0xff);
        bufPos += 5;
    }

    public void writeDouble(double d) {
        if (bufLen - bufPos < 9) nextChunk();
        long l = Double.doubleToLongBits(d);
        buffer[bufPos] = (byte)CBOR.FLOAT_BASE8;
        buffer[bufPos + 1] = (byte)((l >> 56) & 0xff);
        buffer[bufPos + 2] = (byte)((l >> 48) & 0xff);
        buffer[bufPos + 3] = (byte)((l >> 40) & 0xff);
        buffer[bufPos + 4] = (byte)((l >> 32) & 0xff);
        buffer[bufPos + 5] = (byte)((l >> 24) & 0xff);
        buffer[bufPos + 6] = (byte)((l >> 16) & 0xff);
        buffer[bufPos + 7] = (byte)((l >> 8) & 0xff);
        buffer[bufPos + 8] = (byte)(l & 0xff);
        bufPos += 9;
    }

    public void writeObject(Object obj) {

        if (obj == null) {
            write(CBOR.NULL_CODE);
            return;
        }

        Class<?> c = obj.getClass();
        if (c == Byte.class || c == Short.class || c == Integer.class) {
            writeInt(((Number)obj).intValue());
        } else if (c == Long.class) {
            writeLong(((Number)obj).longValue());
        } else if (c == String.class) {
            writeString((String)obj);
        } else if (obj instanceof List) {
            writeList((List)obj);
        } else if (obj instanceof Map) {
            writeMap((Map)obj);
        } else if (obj == Boolean.FALSE) {
            write(CBOR.FALSE_CODE);
        } else if (obj == Boolean.TRUE) {
            write(CBOR.TRUE_CODE);
        } else if (obj == CBOR.BREAK) {
            write(CBOR.BREAK_CODE);
        } else if (c == Float.class) {
            writeFloat((Float)obj);
        } else if (c == Double.class) {
            writeDouble((Double)obj);
        } else if (obj == CBOR.UNKNOWN) {
            write(CBOR.UNKNOWN_CODE);
        }
    }

}
