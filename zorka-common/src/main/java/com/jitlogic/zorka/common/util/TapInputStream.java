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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class TapInputStream extends InputStream {

    public static final int TAP_INIT = 512;
    public static final int TAP_LIMIT = 65536;
    public static final String DEFAULT_ENCODING = "UTF-8";

    private InputStream is;

    private int limit;

    private long pos, mark = -1;
    private byte[] buf;

    private boolean overflow;



    public TapInputStream(InputStream is) {
        this(is, TAP_INIT, TAP_LIMIT);
    }


    public TapInputStream(InputStream is, int init, int limit) {
        this.is = is;
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
    public int read() throws IOException {
        int i = is.read();

        synchronized (this) {
            if (i >= 0) {
                if (pos >= buf.length) {
                    extendBuf((int)pos+1);
                }

                if (!overflow) {
                    buf[(int)pos++] = (byte)i;
                }
            }
        }

        return i;
    }


    @Override
    public int read(byte b[]) throws IOException {
        int n = is.read(b);

        synchronized (this) {
            if (n > 0) {
                if (pos + n >= buf.length) {
                    extendBuf((int)pos+n);
                }

                if (!overflow && pos < buf.length) {
                    System.arraycopy(b, 0, buf, (int)pos, Math.min(n, buf.length-(int)pos));
                }
                pos += n;
            }
        }

        return n;
    }


    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int n = is.read(b, off, len);

        synchronized (this) {
            if (n > 0) {
                if (pos + n >= buf.length) {
                    extendBuf((int)pos+n);
                }

                if (!overflow && pos < buf.length) {
                    System.arraycopy(b, off, buf, (int)pos, Math.min(n, buf.length-(int)pos));
                }

                pos += n;
            }
        }

        return n;
    }


    @Override
    public long skip(long n) throws IOException {

        synchronized (this) {
            pos += (int)n;
        }

        return is.skip(n);
    }


    @Override
    public int available() throws IOException {
        return is.available();
    }


    @Override
    public void close() throws IOException {
        is.close();
    }


    @Override
    public void mark(int readlimit) {

        synchronized (this) {
            mark = pos;
        }

        is.mark(readlimit);
    }


    @Override
    public synchronized void reset() throws IOException {
        is.reset();
        synchronized (this) {
            long m = mark;
            mark = -1;
            if (m != -1) {
                pos = m;
            }
        }
    }


    @Override
    public boolean markSupported() {
        return is.markSupported();
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