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
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.util.Properties;

import static com.jitlogic.zorka.spy.SpyConst.*;

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

        debugLevel = Integer.parseInt(ZorkaConfig.getProperties().getProperty(ZorkaConfig.SPY_DEBUG).trim());

        return instance;
    }


    public static synchronized void cleanup() {
        MainSubmitter.setSubmitter(null);
        instance.shutdown();
        instance = null;
    }

    public static int getDebugLevel() {
        return debugLevel;
    }

    public static boolean isDebugEnabled(int level) {
        return debugLevel >= level;
    }

    private SpyClassTransformer classTransformer;
    private SpySubmitter   submitter;
    private SpyCollector   collector;


    public SpyInstance(Properties props) {
        classTransformer = new SpyClassTransformer();
        collector = new DispatchingCollector();
        submitter = new DispatchingSubmitter(classTransformer, collector);
    }


    public void add(SpyDefinition sdef) {
        classTransformer.add(sdef);
    }


    private void shutdown() {

    }


    public SpyClassTransformer getClassTransformer() {
        return classTransformer;
    }


    public SpySubmitter getSubmitter() {
        return submitter;
    }

}
