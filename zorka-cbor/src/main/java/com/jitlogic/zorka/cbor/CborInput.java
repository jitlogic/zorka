package com.jitlogic.zorka.cbor;

public abstract class CborInput {

    public abstract byte readB();

    public abstract int readI();

    public abstract long readL();

    public abstract int size();

    public abstract boolean eof();

}
