package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.integ.JBossIntegration;
import com.jitlogic.zorka.util.ZorkaLogger;

import javax.management.MBeanServerConnection;
import javax.naming.InitialContext;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MBeanServerRegistry {

    private static final ZorkaLogger log = ZorkaLogger.getLogger(MBeanServerRegistry.class);

    private Map<String,MBeanServerConnection> conns = new ConcurrentHashMap<String, MBeanServerConnection>();

    public MBeanServerRegistry() {
        conns.put("java", ManagementFactory.getPlatformMBeanServer());
    }

    /**
     * Looks for a given MBean server. java and jboss mbean servers are currently available.
     *
     * @param name
     * @return
     */
    public MBeanServerConnection lookup(String name) {
        return conns.get(name);
    }


    public void register(String name, MBeanServerConnection conn) {
        if (!conns.containsKey(name)) {
            conns.put(name, conn);
        } else {
            log.error("MBean server '" + name + "' is already registered.");
        }
    }


    public void unregister(String name) {
        if (conns.containsKey(name)) {
            conns.remove(name);
        } else {
            log.error("Trying to unregister non-existent MBean server '" + name + "'");
        }
    }
}