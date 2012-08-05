package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.util.ZorkaLogger;

import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import java.lang.management.ManagementFactory;

public class MBeanServerRegistry {

    private static final ZorkaLogger log = ZorkaLogger.getLogger(MBeanServerRegistry.class);

    private MBeanServerConnection mbsJava = ManagementFactory.getPlatformMBeanServer();

    private volatile MBeanServerConnection mbsJboss = null;

    /**
     * Looks for a given MBean server. java and jboss mbean servers are currently available.
     *
     * @param name
     * @return
     */
    public MBeanServerConnection lookup(String name) {

        if ("java".equals(name)) {
            return mbsJava;
        }

        if ("jboss".equals(name)) {
            return mbsJboss != null ? mbsJboss : getJBossMbs();
        }

        return null;
    }


    private synchronized MBeanServerConnection getJBossMbs() {
        try {
            InitialContext ctx = new InitialContext();
            MBeanServerConnection server = mbsJboss =
                    (MBeanServerConnection)ctx.lookup("/jmx/rmi/RMIAdaptor");
            return server;
        } catch (Exception e) {
            log.error("Cannot find MBeanServer for JBoss: ", e);
        } catch (NoClassDefFoundError e) {
            log.error("Cannot find MBeanServer for JBoss: ", e);
        }
        return null;
    }
}