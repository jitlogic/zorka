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
package com.jitlogic.zorka.common.util;


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class TapOutputStream extends OutputStream {

    public static final int TAP_INIT = 512;
    public static final int TAP_LIMIT = 65536;
    public static final String DEFAULT_ENCODING = "UTF-8";

    private OutputStream os;

    private int limit;

    private long pos;
    private byte[] buf;

    private boolean overflow;


    public TapOutputStream(OutputStream os) {
        this(os, TAP_INIT, TAP_LIMIT);
    }


    public TapOutputStream(OutputStream os, int init, int limit) {
        this.os = os;
        this.limit = limit;
        this.buf = new byte[init];
    }


    private void extendBuf(int minlen) {
        if (buf.length < limit) {
            int newlen = Math.min(buf.length * 2, limit);

            while (newlen < Math.min(minlen, limit)) {
                newlen = Math.min(newlen * 2, limit);
            }

            buf = ZorkaUtil.clipArray(buf, newlen);

        } else {
            overflow = true;
        }
    }


    @Override
    public void write(int b) throws IOException {
        os.write(b);

        synchronized (this) {
            if (pos >= buf.length) {
                extendBuf((int)pos+1);
            }

            if (!overflow) {
                buf[(int)pos++] = (byte)b;
            }
        }
    }


    @Override
    public void write(byte b[]) throws IOException {
        os.write(b);

        synchronized (this) {
            int n = b.length;

            if (pos + n >= buf.length) {
                extendBuf((int)pos+n);
            }

            if (!overflow && pos < buf.length) {
                System.arraycopy(b, 0, buf, (int)pos, Math.min(n, buf.length-(int)pos));
            }

            pos += n;
        }
    }


    @Override
    public void write(byte b[], int off, int len) throws IOException {
        os.write(b, off, len);

        synchronized (this) {
            if (pos + len >= buf.length) {
                extendBuf((int)pos+len);
            }

            if (!overflow && pos < buf.length) {
                System.arraycopy(b, off, buf, (int)pos, Math.min(len, buf.length-(int)pos));
            }

            pos += len;
        }
    }


    @Override
    public void flush() throws IOException {
        os.flush();
    }


    @Override
    public void close() throws IOException {
        os.close();
    }

    public byte[] asBytes() {
        return  ZorkaUtil.clipArray(buf, (int)Math.min(pos, buf.length));
    }

    public String asString(String encoding) throws UnsupportedEncodingException {
        return new String(asBytes(), encoding != null ? encoding : DEFAULT_ENCODING);
    }

    public String asString() throws UnsupportedEncodingException {
        return asString(null);
    }
}
