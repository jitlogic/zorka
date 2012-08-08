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