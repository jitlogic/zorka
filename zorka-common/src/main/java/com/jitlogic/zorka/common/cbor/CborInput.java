package com.jitlogic.zorka.common.cbor;

public abstract class CborInput {

    public abstract byte peekB();

    public abstract byte readB();

    public abstract byte[] readB(int len);

    public abstract int readI();

    public abstract long readL();

    public abstract int size();

    public abstract boolean eof();

    public abstract int position();

    public abstract void position(int pos);

}
