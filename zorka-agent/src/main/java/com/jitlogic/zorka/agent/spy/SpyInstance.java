/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.agent.spy;


import com.jitlogic.zorka.common.ZorkaLog;
import com.jitlogic.zorka.common.ZorkaLogger;

/**
 * This class binds all parts of spy together to make fully configured instrumentation
 * engine. This is singleton object.
 */
public class SpyInstance {

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

        log.debug(ZorkaLogger.ZSP_CONFIG, "Requested a submitter instance.");

        synchronized (SpyInstance.class) {
            if (null == instance) {
                instance = new SpyInstance();

                log.debug(ZorkaLogger.ZSP_CONFIG, "Setting up submitter: " + instance.getSubmitter());

                MainSubmitter.setSubmitter(instance.getSubmitter());
                MainSubmitter.setTracer(instance().getTracer());
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
     * Creates new instance.
     *
     */
    public SpyInstance() {
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
        tracer.include(matcher);
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
