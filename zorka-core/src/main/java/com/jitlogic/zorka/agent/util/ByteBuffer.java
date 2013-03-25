/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fast and "extensible" byte buffer. It implements fast serialization functions
 * for all major integer types and strings (big-endian variant only).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ByteBuffer {

    /** Default initial buffer size */
    public static final int DEFAULT_BUFSZ = 1024;

    /** COunts overflow occurantes. Overflows are transparent for buffer writers but can be monitored. */
    private static final AtomicLong overflowCount = new AtomicLong(0);

    /** Allocated bute buffer */
    private final byte[] buf;

    /** Length of allocated byte buffer. */
    private final int len;

    /** Current position inside the buffer. */
    private int pos;

    /** Overflow buffer chunks. */
    private List<byte[]> overflows = new ArrayList<byte[]>();


    /**
     * Creates byte buffer of default initial length.
     */
    public ByteBuffer() {
        this(DEFAULT_BUFSZ);
    }

    /**
     * Creates byte buffer of specified length.
     *
     * @param len buffer length
     */
    public ByteBuffer(int len) {
        this.buf = new byte[len];
        this.len = len;
        this.pos = 0;
    }


    /**
     * Creates byte buffer wrapped around passed byte array.
     * This is mostly used for reading data.
     *
     * @param buf byte array
     */
    public ByteBuffer(byte[] buf) {
        this.buf = buf;
        this.len = buf.length;
        this.pos = 0;
    }


    /**
     * Reads single byte from buffer.
     *
     * @return byte read
     */
    public byte getByte() {
        if (pos < len) {
            return buf[pos++];
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    /**
     * Writes single byte to buffer.
     *
     * @param b value to be written
     */
    public void putByte(byte b) {
        if (pos >= len) {
            overflow();
        }
        buf[pos++] = b;
    }


    /**
     * Reads 16-bit integer value from a buffer.
     *
     * @return value read
     */
    public short getShort() {
        if (pos < len-1) {
            return (short)(((buf[pos++] & 0xff) << 8) | ((buf[pos++] & 0xff) << 0));
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    /**
     * Writes 16-bit integer to a buffer.
     *
     * @param x value to be written.
     */
    public void putShort(int x) {
        if (pos >= len-1) {
            overflow();
        }

        buf[pos++] = (byte) (x >> 8);
        buf[pos++] = (byte) (x >> 0);
    }


    /**
     * Reads 32-bit value from a buffer.
     *
     * @return value read
     */
    public int getInt() {
        if (pos < len-3) {
            return ((buf[pos++] & 0xff) << 24)
                 | ((buf[pos++] & 0xff) << 16)
                 | ((buf[pos++] & 0xff) <<  8)
                 | ((buf[pos++] & 0xff) <<  0);
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    /**
     * Writes 32-bit value to a buffer.
     *
     * @param i value to be written.
     */
    public void putInt(int i) {
        if (pos >= len-3) {
            overflow();
        }

        buf[pos++] = (byte) (i >> 24);
        buf[pos++] = (byte) (i >> 16);
        buf[pos++] = (byte) (i >> 8);
        buf[pos++] = (byte) (i >> 0);
    }


    /**
     * Reads 64-bit value from a buffer.
     *
     * @return value to be read.
     */
    public long getLong() {
        if (pos < len-7) {
            return ((buf[pos++] & 0xffL) << 56)
                 | ((buf[pos++] & 0xffL) << 48)
                 | ((buf[pos++] & 0xffL) << 40)
                 | ((buf[pos++] & 0xffL) << 32)
                 | ((buf[pos++] & 0xffL) << 24)
                 | ((buf[pos++] & 0xffL) << 16)
                 | ((buf[pos++] & 0xffL) <<  8)
                 | ((buf[pos++] & 0xffL) <<  0);
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    /**
     * Writes 64-bit value to the buffer.
     *
     * @param l value to be written.
     */
    public void putLong(long l) {
        if (pos >= len-7) {
            overflow();
        }

        buf[pos++] = (byte) (l >> 56);
        buf[pos++] = (byte) (l >> 48);
        buf[pos++] = (byte) (l >> 40);
        buf[pos++] = (byte) (l >> 32);
        buf[pos++] = (byte) (l >> 24);
        buf[pos++] = (byte) (l >> 16);
        buf[pos++] = (byte) (l >> 8);
        buf[pos++] = (byte) (l >> 0);
    }


    /**
     * Reads single precision floating point value.
     *
     * @return double value
     */
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }


    /**
     * Writes double precision floating point value the buffer.
     *
     * @param f value to be written
     */
    public void putFloat(float f) {
        putInt(Float.floatToIntBits(f));
    }


    /**
     * Reads double precision floating point value.
     *
     * @return double value
     */
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }


    /**
     * Writes double precision floating point value the buffer.
     *
     * @param d double
     */
    public void putDouble(double d) {
        putLong(Double.doubleToLongBits(d));
    }


    /**
     * Reads string from a buffer. String is written and null
     * byte is appended at the end.
     *
     * @return string read.
     */
    public String getString() {
        if (pos <= len-2) {
            short strlen = getShort();

            if (strlen > 0) {
                if (pos+strlen <= len) {
                    byte[] bytes = ZorkaUtil.clipArray(buf, pos, strlen);
                    pos += strlen;
                    return new String(bytes);
                } else {
                    throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
                }
            } else {
                return strlen == 0 ? "" : null;
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    /**
     * Writes string to a buffer. String is written and null byte is appended at the end.
     *
     * @param s string to be written.
     */
    public void putString(String s) {

        byte[] bytes = s != null ? s.getBytes() : new byte[0];

        if (pos >= len-1) overflow();

        putShort(s != null ? bytes.length : -1);

        if (bytes.length > 0) {
            if (pos >= len-bytes.length) overflow();
            if (bytes.length >= len) {
                overflows.add(bytes);
            } else {
                System.arraycopy(bytes, 0, buf, pos, bytes.length);
                pos += bytes.length;
            }
        }
    }


    /**
     * Returns content from beginning of the buffer to current
     * position. Any overflows stored aside are appended.
     *
     * @return buffer content
     */
    public byte[] getContent() {
        int blen = pos;

        for (byte[] buf : overflows) {
            blen += buf.length;
        }

        byte[] content = new byte[blen];
        int cpos = 0;

        for (byte[] buf : overflows) {
            int len = buf.length;
            System.arraycopy(buf, 0, content, cpos, len);
            cpos += len;
        }

        System.arraycopy(buf, 0, content, cpos, pos);

        return content;
    }


    /**
     * Cleans up buffer.
     */
    public void reset() {
        overflows.clear();
        pos = 0;
    }


    /**
     * This method is invoked when overflow occurs.
     * It copies written content aside and resets
     * cursor position.
     *
     */
    private void overflow() {
        overflowCount.incrementAndGet();

        overflows.add(ZorkaUtil.clipArray(buf, pos));
        pos = 0;
    }


    /**
     * Returns true if cursor position is at the end of buffer.
     *
     * @return true if cursor position is at the end of buffer.
     */
    public boolean eof() {
        return pos >= len;
    }
}
