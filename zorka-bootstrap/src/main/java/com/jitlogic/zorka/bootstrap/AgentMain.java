package com.jitlogic.zorka.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is responsible for bootstrapping zorka agent.
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class AgentMain {

    private static String homeDir;
    private static ClassLoader systemClassLoader;
    private static ClassLoader zorkaClassLoader;
    public static Agent agent; // TODO uporządkować to !
    private static String serverType = "generic";

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
            //Class<?> clazz = Class.forName("com.jitlogic.zorka.agent.JavaAgent");
            agent = (Agent)clazz.newInstance();
            agent.start();
        } catch (Exception e) {
            throw new RuntimeException("Error starting up agent.", e);
        }

        Thread.currentThread().setContextClassLoader(systemClassLoader);
    }

    private static void setupClassLoader() {
        File lib = new File(homeDir + "/../lib");

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
        zorkaClassLoader = new URLClassLoader(urls, systemClassLoader);
        Thread.currentThread().setContextClassLoader(systemClassLoader);
    }

    public static String getHomeDir() {
        return homeDir;
    }

    public static void logStart(long id) {
        agent.logStart(id);
    }

    public static void logStart(Object[] args, long id) {
        agent.logStart(args, id);
    }

    public static void logCall(long id) {
        agent.logCall(id);
    }

    public static void logError(long id) {
        agent.logError(id);
    }

    public static Agent getAgent() {
        return agent;
    }

}
