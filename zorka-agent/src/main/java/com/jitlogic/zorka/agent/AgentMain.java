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

            agent.getSpyInstance().start();
        }
    }
}
