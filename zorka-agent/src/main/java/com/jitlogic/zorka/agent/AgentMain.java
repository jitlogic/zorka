package com.jitlogic.zorka.agent;

import java.io.File;
import java.io.FileFilter;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is responsible for bootstrapping zorka agent.
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AgentMain {

    private static String homeDir;
    private static MBeanServerRegistry mBeanServerRegistry;
    public static JavaAgent agent;

    public static void premain(String args, Instrumentation instr) {
        String[] argv = args.split(",");
        homeDir = argv[0];

        startZorkaAgent();

        if (agent != null && agent.getSpyTransformer() != null) {
            instr.addTransformer(agent.getSpyTransformer(), false);
        }
    }

    private static void startZorkaAgent() {


        mBeanServerRegistry = new MBeanServerRegistry(
            "yes".equalsIgnoreCase(ZorkaConfig.get("zorka.mbs.autoregister", "yes")));
        AgentGlobals.setMBeanServerRegistry(mBeanServerRegistry);

        agent = new JavaAgent();
        agent.start();
    }

    public static String getHomeDir() {
        return homeDir;
    }

}
