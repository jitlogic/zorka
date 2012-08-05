package com.jitlogic.zorka.jboss;

import com.jitlogic.zorka.agent.JavaAgent;
import org.jboss.system.ServiceMBeanSupport;

import javax.management.MBeanServerConnection;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class Zorka5Service extends ServiceMBeanSupport implements Zorka5ServiceMBean {


    protected void startService() {
        log.info("Starting ZORKA-5 Service");
        MBeanServerConnection conn = this.getServer();

        if (JavaAgent.getAgent() != null) {
            JavaAgent.getAgent().getMBeanServerRegistry().register("jboss", getServer());
            log.info("Registered JBoss MBean server as 'jboss'.");
        } else {
            log.warn("Zorka agent not started ?");
        }
    }

    protected void stopService() {
        if (JavaAgent.getAgent() != null) {
            log.info("Unregistering 'jboss' MBean server.");
            JavaAgent.getAgent().getMBeanServerRegistry().unregister("jboss");
        } else {
            log.warn("Zorka agent not started ?");
        }
    }

    public boolean isRunning() {
        return JavaAgent.getAgent() != null;
    }
}
