/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ByteBuffer {

    public static final int DEFAULT_BUFSZ = 1024;

    private static final AtomicLong overflowCount = new AtomicLong(0);
    private static final AtomicLong overflowLength = new AtomicLong(0);

    private final byte[] buf;
    private final int len;
    private int pos;

    private List<byte[]> overflows = new ArrayList<byte[]>();


    public ByteBuffer() {
        this(DEFAULT_BUFSZ);
    }


    public ByteBuffer(int len) {
        this.buf = new byte[len];
        this.len = len;
        this.pos = 0;
    }


    public ByteBuffer(byte[] buf) {
        this.buf = buf;
        this.len = buf.length;
        this.pos = 0;
    }


    public byte getByte() {
        if (pos < len) {
            return buf[pos++];
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    public void putByte(byte b) {
        if (pos >= len) {
            overflow();
        }
        buf[pos++] = b;
    }


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


    public void putInt(int i) {
        if (pos >= len-3) {
            overflow();
        }

        buf[pos++] = (byte) (i >> 24);
        buf[pos++] = (byte) (i >> 16);
        buf[pos++] = (byte) (i >> 8);
        buf[pos++] = (byte) (i >> 0);
    }


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


    public String getString() {
        if (pos < len) {
            int end = pos;

            while (end < len && buf[end] != 0) {
                end++;
            }

            byte[] bstr = ZorkaUtil.clipArray(buf, pos, end-pos);

            pos = end+1;

            return new String(bstr);
        } else {
            throw new ArrayIndexOutOfBoundsException("Trying to read past the end of buffer.");
        }
    }


    public void putString(String s) {
        byte[] bstr = s != null ? s.getBytes() : new byte[0];

        if (pos >= len-bstr.length) {
            overflow();
        }

        if (bstr.length >= len) {
            overflows.add(bstr);
        } else {
            System.arraycopy(bstr, 0, buf, pos, bstr.length);
            pos += bstr.length;
            buf[pos++] = 0;
        }
    }


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


    public void reset() {
        overflows.clear();
        pos = 0;
    }


    private void overflow() {
        overflowCount.incrementAndGet();
        overflowLength.addAndGet(pos);

        overflows.add(ZorkaUtil.clipArray(buf, pos));
        pos = 0;
    }
}
