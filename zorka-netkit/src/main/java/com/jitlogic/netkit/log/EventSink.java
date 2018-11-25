package com.jitlogic.netkit.log;

public abstract class EventSink {

    public long time() {
        return System.nanoTime();
    }

    public void call() {
        call(0);
    }

    public abstract void call(long t);

    public void error() {
        error(0);
    }

    public abstract void error(long t);

    public abstract String getName();

    public abstract long getCalls();

    public abstract long getErrors();

    public abstract long getTime();
}
