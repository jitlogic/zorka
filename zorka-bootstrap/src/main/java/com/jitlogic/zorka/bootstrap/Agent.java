package com.jitlogic.zorka.bootstrap;

import java.lang.instrument.ClassFileTransformer;

/**
 */
public interface Agent {

    public void start();

    public void stop();

    public ClassFileTransformer getSpyTransformer();

    public void logStart(long id);

    public void logStart(Object[] args, long id);

    public void logCall(long id);

    public void logError(long id);
}
