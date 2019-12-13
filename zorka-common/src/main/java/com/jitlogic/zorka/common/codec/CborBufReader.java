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

import static com.jitlogic.zorka.cbor.CBOR.*;

/**
 *
 */
public class CborBufReader {

    private byte[] buf;
    private int pos;

    // TODO implement safe version of this reader, merge with CborDataReader

    public CborBufReader(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
    }

    public byte[] getRawBytes() {
        return buf;
    }

    public int position() {
        return pos;
    }


    public void position(int pos) {
        if (pos < 0 || pos > buf.length) {
            throw new ZorkaRuntimeException("Try ing to set illegal position on direct buf");
        }
        this.pos = pos;
    }


    public int size() { return buf.length; }


    public byte read() {
        byte b = buf[pos];
        pos++;
        return b;
    }


    public long readLong() {
        int b = buf[pos] & 0xff, t = b & TYPE_MASK;
        long v = (b & VALU_MASK);
        pos++;

        switch ((int)v) {
            case UINT_CODE1:
                v = buf[pos] & 0xffL;
                pos++;
                break;
            case UINT_CODE2:
                v = ((buf[pos] & 0xffL) << 8) |
                    (buf[pos+1] & 0xffL);
                pos += 2;
                break;
            case UINT_CODE4:
                v = ((buf[pos] & 0xffL) << 24) |
                    ((buf[pos+1] & 0xffL) << 16) |
                    ((buf[pos+2] & 0xffL) << 8) |
                    ((buf[pos+3] & 0xffL));
                pos += 4;
                break;
            case UINT_CODE8:
                v = ((buf[pos] & 0xffL) << 56) |
                    ((buf[pos+1] & 0xffL) << 48) |
                    ((buf[pos+2] & 0xffL) << 40) |
                    ((buf[pos+3] & 0xffL) << 32) |
                    ((buf[pos+4] & 0xffL) << 24) |
                    ((buf[pos+5] & 0xffL) << 16) |
                    ((buf[pos+6] & 0xffL) << 8) |
                    ((buf[pos+7] & 0xffL));
                pos += 8;
                break;
            default:
                if (v > UINT_CODE8) {
                    pos--;
                    throw new ZorkaRuntimeException("Invalid prefix code: " + b);
                }
        }

        return t == NINT_BASE ? -v-1 : v;
    }


    public int readInt() {
        int b = buf[pos] & 0xff; pos++;
        int v = b & VALU_MASK, t = b & TYPE_MASK;

        switch (v) {
            case UINT_CODE1:
                v = buf[pos] & 0xff;
                pos++;
                break;
            case UINT_CODE2:
                v = ((buf[pos] & 0xff) << 8) |
                    (buf[pos+1] & 0xff);
                pos += 2;
                break;
            case UINT_CODE4:
                v = ((buf[pos] & 0xff) << 24) |
                    ((buf[pos+1] & 0xff) << 16) |
                    ((buf[pos+2] & 0xff) << 8) |
                    ((buf[pos+3] & 0xff));
                pos += 4;
                break;
            case UINT_CODE8:
                pos--;
                throw new ZorkaRuntimeException("Expected int but encountered long at pos " + pos);
            default:
                if (v > UINT_CODE8) {
                    pos--;
                    throw new ZorkaRuntimeException("Invalid prefix code: " + b);
                }
        }

        return t == NINT_BASE ? -v-1 : v;
    }


    public byte[] readBytes() {
        // TODO handle variable length byte arrays (if needed)
        int type = peekType();
        if (type != BYTES_BASE && type != STR_BASE) {
            throw new ZorkaRuntimeException("Expected byte array but got type=" + type);
        }
        int len = readInt();
        if (len > buf.length - pos) {
            throw new ZorkaRuntimeException("Unexpected end of buffer.");
        }
        byte[] rslt = new byte[len];
        System.arraycopy(buf, pos, rslt, 0, len);
        pos += len;
        return rslt;
    }


    public String readStr() {
        int type = peekType();
        if (type != STR_BASE) {
            throw new ZorkaRuntimeException("Expected string data but got type=" + type);
        }
        byte[] buf = readBytes();
        return new String(buf);
    }

    public int readTag() {
        int type = peekType();
        if (type != TAG_BASE) {
            throw new ZorkaRuntimeException(String.format("Expected tag, got type %02x", type));
        }
        return readInt();
    }

    public int peek() {
        return buf[pos] & 0xff;
    }


    public int peekType() {
        return (buf[pos] & 0xff) & TYPE_MASK;
    }


    public long readRawLong(boolean littleEndian) {
        long rslt;
        if (littleEndian) {
            rslt = (buf[pos] & 0xffL)
                | ((buf[pos+1] & 0xffL) << 8)
                | ((buf[pos+2] & 0xffL) << 16)
                | ((buf[pos+3] & 0xffL) << 24)
                | ((buf[pos+4] & 0xffL) << 32)
                | ((buf[pos+5] & 0xffL) << 40)
                | ((buf[pos+6] & 0xffL) << 48)
                | ((buf[pos+7] & 0xffL) << 56);
        } else {
            rslt = ((buf[pos] & 0xffL) << 56)
                |  ((buf[pos+1] & 0xffL) << 48)
                |  ((buf[pos+2] & 0xffL) << 40)
                |  ((buf[pos+3] & 0xffL) << 32)
                |  ((buf[pos+4] & 0xffL) << 24)
                |  ((buf[pos+5] & 0xffL) << 16)
                |  ((buf[pos+6] & 0xffL) << 8)
                |   (buf[pos+7] & 0xffL);
        }
        pos += 8;
        return rslt;
    }


}
