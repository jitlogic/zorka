/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.AgentConfig;
import com.jitlogic.zorka.core.AgentInstance;
import com.jitlogic.zorka.core.ZorkaControl;
import com.jitlogic.zorka.core.spy.MainSubmitter;
import com.jitlogic.zorka.core.spy.RealSpyRetransformer;
import com.jitlogic.zorka.core.spy.SpyClassLookup;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * This class is responsible for bootstrapping zorka agent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentMain {

    /**
     * This is entry method of java agent.
     *
     * @param args            arguments (supplied via -javaagent:/path/to/agent.jar=arguments)
     * @param instrumentation reference to JVM instrumentation interface
     */
    public static void premain(String args, Instrumentation instrumentation) {

        String home = System.getProperty("zorka.home.dir", args);

        if (new File(home, "zorka.properties").canRead()) {
            System.out.println("ZORKA agent starting at " + home);
        } else {
            System.out.println("ERROR: " + home + " is not a proper ZORKA home directory.");
        }

        String agentJar = System.getProperty("zorka.agent.jar");
        if (agentJar != null) {
            try {
                if (new File(agentJar).canRead()) {
                    JarFile jf = new JarFile(agentJar);
                    instrumentation.appendToBootstrapClassLoaderSearch(jf);
                    System.out.println("ZORKA: Added " + jf + " to system boot classpath.");
                } else {
                    System.out.println("ERROR: File " + agentJar + " is not readable.");
                }
            } catch (IOException e) {
                System.out.println("ERROR: Cannot open agent JAR file: " + agentJar + ": " + e.getMessage());
            }
        }

        AgentConfig config = new AgentConfig(ZorkaUtil.path(home));

        if (config.boolCfg("spy.reflection.enabled", true)) {
            SpyClassLookup.INSTANCE = new SpyClassLookup();
        } else {
            SpyClassLookup.INSTANCE = new SpyClassLookup(instrumentation);
        }

        AgentInstance instance = new AgentInstance(config, new RealSpyRetransformer(instrumentation, config));

        instance.start();

        if (instance.getConfig().boolCfg("spy", true)) {
            instrumentation.addTransformer(instance.getClassTransformer(), true);
            MainSubmitter.setSubmitter(instance.getSubmitter());
            MainSubmitter.setTracer(instance.getTracer());
        }

        instance.getMBeanServerRegistry().registerZorkaControl(
                new ZorkaControl("java", "zorka:type=ZorkaControl,name=ZorkaControl", instance));
    }

}
