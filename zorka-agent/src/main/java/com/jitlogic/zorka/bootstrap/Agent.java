package com.jitlogic.zorka.bootstrap;

import javax.management.MBeanServerConnection;
import java.lang.instrument.ClassFileTransformer;

/**
 */
public interface Agent {

    public void start();

    public void stop();

    public ClassFileTransformer getSpyTransformer();

    public void registerMbs(String name, MBeanServerConnection conn, ClassLoader classLoader);

    public void unregisterMbs(String name);
}
