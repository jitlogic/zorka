package com.jitlogic.zorka.common.cbor;

import com.jitlogic.zorka.common.util.ZorkaRuntimeException;

public class ByteArrayCborInput extends CborInput {

    private byte[] buf;
    private int pos;
    private int offs;
    private int lim;

    public ByteArrayCborInput(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public ByteArrayCborInput(byte[] buf, int offs, int size) {
        this.buf = buf;
        this.pos = offs;
        this.offs = offs;
        this.lim = offs + size;
    }

    @Override
    public byte peekB() {
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        return buf[pos];
    }

    public byte readB() {
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        return buf[pos++];
    }

    @Override
    public byte[] readB(int len) {
        if (pos+len > lim) throw new ZorkaRuntimeException("Unexpected EOD");
        byte[] b = new byte[len];
        System.arraycopy(buf, pos, b, 0, len);
        pos += len;
        return b;
    }

    @Override
    public int readI() {
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        return buf[pos++] & 0xff;
    }

    @Override
    public long readL() {
        if (pos >= lim) throw new ZorkaRuntimeException("Unexpected EOD");
        return buf[pos++] & 0xffL;
    }

    public int size() {
        return lim - offs;
    }

    public boolean eof() {
        return pos < lim;
    }

    @Override
    public int position() {
        return pos;
    }

    @Override
    public void position(int pos) {
        this.pos = pos;
    }
}
