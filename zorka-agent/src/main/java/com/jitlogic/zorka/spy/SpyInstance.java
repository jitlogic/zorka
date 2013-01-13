/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;


import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.tracer.Tracer;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.Properties;

import static com.jitlogic.zorka.spy.SpyLib.SPD_CONFIG;

/**
 * This class binds all parts of spy together to make fully configured instrumentation
 * engine. This is singleton object.
 */
public class SpyInstance {

    /** Debug level for spy components.  */
    private static volatile int debugLevel;

    /** Logger */
    private static ZorkaLog log = ZorkaLogger.getLog(SpyInstance.class);

    /** Spy instance reference */
    private static volatile SpyInstance instance;

    /** Reference to instance's class transformer */
    private SpyClassTransformer classTransformer;

    /** Reference to instance's main submitter (installed in MainSubmitter when instance starts) */
    private DispatchingSubmitter   submitter;

    /** Tracer */
    private Tracer tracer;

    /**
     * Returns spy instance. Creates one if called for the first time.
     * Configures MainSubmitter to submit values to newly created instance.
     *
     * @return spy instance
     */
    public static SpyInstance instance() {

        if (isDebugEnabled(SPD_CONFIG)) {
            log.debug("Requested a submitter instance.");
        }

        synchronized (SpyInstance.class) {
            if (null == instance) {
                instance = new SpyInstance(ZorkaConfig.getProperties());

                if (isDebugEnabled(SPD_CONFIG)) {
                    log.debug("Setting up submitter: " + instance.getSubmitter());
                }

                MainSubmitter.setSubmitter(instance.getSubmitter());
                MainSubmitter.setTracer(instance().getTracer());

                debugLevel = Integer.parseInt(ZorkaConfig.getProperties().getProperty("spy.debug").trim());
            }

        }

        return instance;
    }


    /**
     * Stops spy instance and deconfigures MainSubmitter.
     */
    public static void cleanup() {
        synchronized (SpyInstance.class) {
            MainSubmitter.setSubmitter(null);
            instance = null;
        }
    }


    /**
     * Returns true if log level is higher or equal to that passed with argument.
     *
     * @param level log level compared to configuration setting
     *
     * @return true or false
     */
    public static boolean isDebugEnabled(int level) {
        return debugLevel >= level;
    }


    /**
     * Creates new instance.
     *
     * @param props configuration properties.
     */
    public SpyInstance(Properties props) {
        tracer = new Tracer();
        classTransformer = new SpyClassTransformer(tracer);
        submitter = new DispatchingSubmitter(classTransformer);
    }


    /**
     * Registers new sdef in spy instance.
     *
     * @param sdef spy definition
     */
    public void add(SpyDefinition sdef) {
        classTransformer.add(sdef);
    }


    /**
     * Adds new matcher for tracer.
     *
     * @param matcher matcher
     */
    public void include(SpyMatcher matcher) {
        classTransformer.add(matcher);
    }


    /**
     * Returns instance's class transformer.
     *
     * @return class transformer
     */
    public SpyClassTransformer getClassTransformer() {
        return classTransformer;
    }


    /**
     * Returns instance's submitter.
     *
     * @return main submitter
     */
    public DispatchingSubmitter getSubmitter() {
        return submitter;
    }

    /**
     *
     */
    public Tracer getTracer() {
        return tracer;
    }
}
