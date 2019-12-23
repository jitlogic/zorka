package com.jitlogic.zorka.common.cbor;

import java.io.InputStream;

public class Base64FormattingStream extends InputStream {

    private CborInput input;

    private int b64length;
    private int remaining;

    private int tbuf;
    private int tsz;

    private final static int[] ENC;

    public Base64FormattingStream(CborInput input) {
        this.input = input;
        this.remaining = input.size();
        this.b64length = ((remaining + 2) / 3) * 4;
    }

    private static final int PAD1 = '=' << 24;
    private static final int PAD2 = ('=' << 16) | ('=' << 24);

    private boolean next() {
        if (remaining >= 3) {
            int i0 = input.readI();
            int i1 = input.readI();
            int i2 = input.readI();
            tbuf = ENC[i0>>2] |
                    (ENC[((i0&0x03)<<4)|((i1>>>4)&0x0f)]<<8) |
                    (ENC[((i1&0x0f)<<2)|((i2>>>6)&0x03)]<<16) |
                    (ENC[i2&0x3f]<<24);
            remaining -= 3; tsz = 4;
            return true;
        } else if (remaining == 2) {
            int i0 = input.readI();
            int i1 = input.readI();
            tbuf = ENC[i0>>2] |
                    (ENC[((i0&0x03)<<4)|((i1>>4)&0x0f)]<<8) |
                    (ENC[(i1&0x0f)<<2]<<16) | PAD1;
            remaining -= 3; tsz = 4;
            return true;
        } else if (remaining == 1) {
            int i0 = input.readI();
            tbuf = ENC[i0>>2] | (ENC[(i0&0x03)<<4]<<8) | PAD2;
            remaining -= 3; tsz = 4;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) {

        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int pos = off;
        int lim = off + len;

        while (tsz > 0 && pos < lim) {
            b[pos++] = (byte)(tbuf & 0xff);
            tsz--; tbuf >>>= 8;
        }

        if (pos == lim) {
            b64length -= pos-off;
            return pos-off;
        }

        while (pos < lim-3) {

            if (!next()) {
                b64length -= pos-off;
                return pos-off;
            }

            b[pos] = (byte)(tbuf & 0xff);
            b[pos+1] = (byte)((tbuf >>> 8) & 0xff);
            b[pos+2] = (byte)((tbuf >>> 16) & 0xff);
            b[pos+3] = (byte)((tbuf >>> 24) & 0xff);

            pos += 4;
        }

        tsz = 0;
        if (pos == lim || !next()) {
            b64length -= pos-off;
            return pos-off;
        }

        while (pos < lim) {
            b[pos++] = (byte)(tbuf & 0xff);
            tsz--; tbuf >>>= 8;
        }

        b64length -= pos-off;
        return pos-off;
    }


    public int read() {
        if (tsz == 0 && !next()) return -1;

        int rslt = tbuf & 0xff;

        b64length--;
        tsz--;
        tbuf >>>= 8;

        return rslt;
    }

    public int available() {
        return b64length;
    }

    static {
        ENC = new int[64];
        for (int i = 0; i < 26; i++) ENC[i] = 'A'+i;
        for (int i = 0; i < 26; i++) ENC[i+26] = 'a'+i;
        for (int i = 0; i < 10; i++) ENC[i+52] = '0'+i;
        ENC[62] = '+'; ENC[63] = '/';
    }
}
