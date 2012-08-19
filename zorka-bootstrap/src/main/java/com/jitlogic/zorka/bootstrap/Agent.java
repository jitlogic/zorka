package com.jitlogic.zorka.bootstrap;

import java.lang.instrument.ClassFileTransformer;

/**
 */
public interface Agent {

    public void start();

    public void stop();

    public ClassFileTransformer getSpyTransformer();

}
