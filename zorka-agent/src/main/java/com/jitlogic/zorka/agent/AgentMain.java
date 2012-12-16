package com.jitlogic.zorka.agent;

import java.lang.instrument.Instrumentation;

/**
 * This class is responsible for bootstrapping zorka agent.
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AgentMain {

    private static String homeDir;
    private static MBeanServerRegistry mBeanServerRegistry;
    public static AgentInstance agent;

    public static void premain(String args, Instrumentation instr) {
        String[] argv = args.split(",");
        homeDir = argv[0];

        ZorkaConfig.loadProperties(homeDir);

        startZorkaAgent();

        if (agent != null && agent.getSpyTransformer() != null) {
            // TODO what about retransforming ? (call it with reflection?)
            instr.addTransformer(agent.getSpyTransformer());

            agent.getSpyInstance().start();
        }
    }

    private static void startZorkaAgent() {


        mBeanServerRegistry = new MBeanServerRegistry(
            "yes".equalsIgnoreCase(ZorkaConfig.getProperties().getProperty(ZorkaConfig.ZORKA_MBS_AUTOREG)));
        AgentInstance.setMBeanServerRegistry(mBeanServerRegistry);

        agent = AgentInstance.instance();
    }

}
