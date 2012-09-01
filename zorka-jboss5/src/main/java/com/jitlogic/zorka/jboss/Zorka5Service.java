/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.jboss;

import com.jitlogic.zorka.bootstrap.AgentMain;
import org.jboss.system.ServiceMBeanSupport;

import javax.management.MBeanServerConnection;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class Zorka5Service extends ServiceMBeanSupport implements Zorka5ServiceMBean {


    protected void startService() {
        log.info("Starting ZORKA-5 Service");
        MBeanServerConnection conn = this.getServer();

        if (AgentMain.getAgent() != null) {
            AgentMain.getAgent().registerMbs("jboss", getServer());
            log.info("Registered JBoss MBean server as 'jboss'.");
        } else {
            log.warn("Zorka agent not started ?");
        }
    }

    protected void stopService() {
        if (AgentMain.getAgent() != null) {
            log.info("Unregistering 'jboss' MBean server.");
            AgentMain.getAgent().unregisterMbs("jboss");
        } else {
            log.warn("Zorka agent not started ?");
        }
    }

    public boolean isRunning() {
        return AgentMain.getAgent() != null;
    }
}
