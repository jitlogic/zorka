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
    private static ClassLoader systemClassLoader;
    private static AgentClassLoader zorkaClassLoader;
    public static JavaAgent agent;

    public static void premain(String args, Instrumentation instr) {
        String[] argv = args.split(",");
        homeDir = argv[0];

        setupClassLoader();
        startZorkaAgent();

        if (agent != null && agent.getSpyTransformer() != null) {
            instr.addTransformer(agent.getSpyTransformer(), false);
        }
    }

    private static void startZorkaAgent() {
        Thread.currentThread().setContextClassLoader(zorkaClassLoader);

        try {
            Class<?> clazz = zorkaClassLoader.loadClass("com.jitlogic.zorka.agent.JavaAgent");
            agent = (JavaAgent)clazz.newInstance();
            agent.start();
        } catch (Exception e) {
            throw new RuntimeException("Error starting up agent.", e);
        } finally {
        }

        Thread.currentThread().setContextClassLoader(systemClassLoader);
    }

    private static void setupClassLoader() {
        File lib = new File(homeDir + File.separator + "lib");

        if (!lib.isDirectory()) {
            throw new RuntimeException("" + lib.getPath() + " should exist and be a directory !");
        }

        File[] libs = lib.listFiles((FileFilter)null);
        URL[] urls = new URL[libs.length];

        try {
            for (int i = 0; i < libs.length; i++) {
                urls[i] = libs[i].toURI().toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL for Zorka component", e);
        }

        systemClassLoader = Thread.currentThread().getContextClassLoader();
        zorkaClassLoader = new AgentClassLoader(urls, systemClassLoader);
        Thread.currentThread().setContextClassLoader(systemClassLoader);
    }

    public static String getHomeDir() {
        return homeDir;
    }

    public static void addJarURL(URL url) {
        if (zorkaClassLoader != null)
            zorkaClassLoader.addURL(url);
    }


}
