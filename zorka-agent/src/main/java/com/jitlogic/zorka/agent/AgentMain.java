/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.mbeans.MBeanServerRegistry;

import java.lang.instrument.Instrumentation;

/**
 * This class is responsible for bootstrapping zorka agent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentMain {

    /** Zorka home directory (passed as -javaagent argument) */
    private static String homeDir;

    /** Agent instance */
    public static AgentInstance agent;

    /**
     * This is entry method of java agent.
     *
     * @param args arguments (supplied via -javaagent:/path/to/agent.jar=arguments)
     *
     * @param instr reference to JVM instrumentation interface
     */
    public static void premain(String args, Instrumentation instr) {
        String[] argv = args.split(",");
        homeDir = argv[0];

        ZorkaConfig.loadProperties(homeDir);

        MBeanServerRegistry mBeanServerRegistry = new MBeanServerRegistry(
            "yes".equalsIgnoreCase(ZorkaConfig.getProperties().getProperty("zorka.mbs.autoregister")));
        AgentInstance.setMBeanServerRegistry(mBeanServerRegistry);

        agent = AgentInstance.instance();

        if (agent != null && agent.getSpyTransformer() != null) {
            instr.addTransformer(agent.getSpyTransformer());

        }
    }
}
