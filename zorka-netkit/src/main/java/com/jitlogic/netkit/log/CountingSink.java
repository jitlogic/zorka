package com.jitlogic.netkit.log;

import java.util.concurrent.atomic.AtomicLong;

public class CountingSink extends EventSink {

    private final String name;

    private final AtomicLong calls = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong time = new AtomicLong(0);

    public CountingSink(String name) {
        this.name = name;
    }

    @Override
    public void call(long t) {
        calls.incrementAndGet();
        if (t > 0) time.addAndGet(time()-t);
    }

    @Override
    public void error(long t) {
        errors.incrementAndGet();
        if (t > 0) time.addAndGet(time()-t);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getCalls() {
        return calls.get();
    }

    @Override
    public long getErrors() {
        return errors.get();
    }

    @Override
    public long getTime() {
        return time.get();
    }
}
