package com.jitlogic.zorka.jboss;

import org.jboss.system.ServiceMBean;

public interface Zorka5ServiceMBean extends ServiceMBean {

    public boolean isRunning();

}
