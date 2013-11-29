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

import com.jitlogic.zorka.core.AgentConfig;
import com.jitlogic.zorka.core.AgentInstance;
import com.jitlogic.zorka.core.spy.MainSubmitter;

import java.lang.instrument.Instrumentation;

/**
 * This class is responsible for bootstrapping zorka agent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentMain {

    private static volatile AgentInstance instance;

    public static AgentInstance getInstance() {
        return instance;
    }

    /**
     * This is entry method of java agent.
     *
     * @param args            arguments (supplied via -javaagent:/path/to/agent.jar=arguments)
     * @param instrumentation reference to JVM instrumentation interface
     */
    public static void premain(String args, Instrumentation instrumentation) {

        String home = System.getProperties().getProperty("zorka.home.dir", args);

        instance = new AgentInstance(new AgentConfig(home), instrumentation);
        instance.start();

        if (instance.getConfig().boolCfg("spy", true)) {
            instrumentation.addTransformer(instance.getClassTransformer());
            MainSubmitter.setSubmitter(instance.getSubmitter());
            MainSubmitter.setTracer(instance.getTracer());
        }
    }

}
