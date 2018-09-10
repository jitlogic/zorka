package com.jitlogic.zorka.cbor;

public class ByteArrayCborInput extends CborInput {

    private byte[] buf;
    private int pos, lim;

    public ByteArrayCborInput(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public ByteArrayCborInput(byte[] buf, int offs, int size) {
        this.buf = buf;
        this.pos = offs;
        this.lim = offs + size;
    }

    public byte readB() { return pos < lim ? buf[pos++] : -1; }

    public int readI() {
        return pos < lim ? buf[pos++] & 0xff : -1;
    }

    public long readL() {
        return pos < lim ? buf[pos++] & 0xff : -1;
    }

    public int size() {
        return lim - pos;
    }
}
