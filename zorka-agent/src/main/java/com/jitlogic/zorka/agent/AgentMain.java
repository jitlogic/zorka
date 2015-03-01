/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.core.spy.SpyRetransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * This class is responsible for bootstrapping zorka agent.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AgentMain {

    private static volatile AgentInstance instance;

    private static boolean supportsRetransform(Instrumentation instrumentation) throws Exception {
        for (Method m : instrumentation.getClass().getMethods()) {
            if ("isRetransformClassesSupported".equals(m.getName())) {
                return (Boolean) m.invoke(instrumentation);
            }
        }
        return false;
    }

    private static SpyRetransformer instantiateRetransformer(Instrumentation instrumentation, AgentConfig config,
                                                             String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        for (Constructor<?> constructor : clazz.getConstructors()) {
            Class<?>[] args = constructor.getParameterTypes();
            if (args.length == 2) {
                return (SpyRetransformer) constructor.newInstance(instrumentation, config);
            }
        }

        throw new IllegalArgumentException("Cannot instantiate retransformer of class " + className);
    }

    private static void addTransformer(Instrumentation instrumentation, ClassFileTransformer transformer,
                                       boolean retransformSupported) throws Exception {
        Method atm = null;

        for (Method m : Instrumentation.class.getMethods()) {
            if ("addTransformer".equals(m.getName())) {
                atm = m;
                if (m.getParameterTypes().length == 2 || !retransformSupported) {
                    break;
                }
            }
        }

        if (atm.getParameterTypes().length == 2) {
            atm.invoke(instrumentation, transformer, retransformSupported);
        } else {
            atm.invoke(instrumentation, transformer);
        }

    }

    /**
     * This is entry method of java agent.
     *
     * @param args            arguments (supplied via -javaagent:/path/to/agent.jar=arguments)
     * @param instrumentation reference to JVM instrumentation interface
     */
    public static void premain(String args, Instrumentation instrumentation) throws Exception {

        String home = System.getProperties().getProperty("zorka.home.dir", args);

        boolean retransformSupported = supportsRetransform(instrumentation);

        AgentConfig config = new AgentConfig(ZorkaUtil.path(home));
        instance = new AgentInstance(config, instantiateRetransformer(instrumentation, config,
                "com.jitlogic.zorka.core.spy." + (retransformSupported ? "RealSpyRetransformer" : "DummySpyRetransformer")));

        instance.start();

        if (instance.getConfig().boolCfg("spy", true)) {
            addTransformer(instrumentation, instance.getClassTransformer(), retransformSupported);
            MainSubmitter.setSubmitter(instance.getSubmitter());
            MainSubmitter.setTracer(instance.getTracer());
        }

        instance.getMBeanServerRegistry().registerZorkaControl(
                new ZorkaControl("java", "zorka:type=ZorkaControl,name=ZorkaControl", instance));
    }

}
