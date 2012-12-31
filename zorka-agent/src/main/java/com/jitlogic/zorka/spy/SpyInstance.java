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
import com.jitlogic.zorka.spy.processors.CollectQueueProcessor;
import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

import java.util.Properties;

import static com.jitlogic.zorka.api.SpyLib.SPD_CONFIG;

/**
 * This class binds all parts of spy together to make fully configured instrumentation
 * engine.
 */
public class SpyInstance {

    private static int debugLevel = 0;
    private static ZorkaLog log = null;

    private static SpyInstance instance = null;


    public static synchronized SpyInstance instance() {

        if (null == log) {
            log = ZorkaLogger.getLog(SpyInstance.class);
        }

        if (isDebugEnabled(SPD_CONFIG)) {
            log.debug("Requested a submitter instance.");
        }

        if (null == instance) {

            instance = new SpyInstance(ZorkaConfig.getProperties());

        }

        if (isDebugEnabled(SPD_CONFIG)) {
            log.debug("Setting up submitter: " + instance.getSubmitter());
        }

        MainSubmitter.setSubmitter(instance.getSubmitter());

        debugLevel = Integer.parseInt(ZorkaConfig.getProperties().getProperty("spy.debug").trim());

        return instance;
    }


    public static synchronized void cleanup() {
        MainSubmitter.setSubmitter(null);
        instance.stop();
        instance = null;
    }


    public static int getDebugLevel() {
        return debugLevel;
    }


    public static boolean isDebugEnabled(int level) {
        return debugLevel >= level;
    }


    private SpyClassTransformer classTransformer;
    private DispatchingSubmitter   submitter;


    public SpyInstance(Properties props) {
        classTransformer = new SpyClassTransformer();
        submitter = new DispatchingSubmitter(classTransformer, new CollectQueueProcessor());
    }


    public void add(SpyDefinition sdef) {
        classTransformer.add(sdef);
    }


    public void start() {
        submitter.start();
    }


    public void stop() {
        if (submitter != null) {
            submitter.stop();
        }
    }


    public SpyClassTransformer getClassTransformer() {
        return classTransformer;
    }


    public DispatchingSubmitter getSubmitter() {
        return submitter;
    }

}
