/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy;


import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.AgentConfig;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

public class RealSpyRetransformer implements SpyRetransformer {

    private static final ZorkaLog log = ZorkaLogger.getLog(RealSpyRetransformer.class);

    private Instrumentation instrumentation;

    private boolean matchMethods;

    public RealSpyRetransformer(Instrumentation instrumentation, AgentConfig config) {
        this.instrumentation = instrumentation;
        matchMethods = config.boolCfg("zorka.retransform.match.methods", false);
        log.info(ZorkaLogger.ZSP_CONFIG, "Enabling spy retransformer. Full online reconfiguration should be possible.");
    }


    @Override
    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet, boolean isSdef) {
        if (instrumentation == null || !instrumentation.isRetransformClassesSupported()) {
            log.warn(ZorkaLogger.ZSP_CONFIG, "Class retransform is not supported. Skipping.");
            return false;
        }

        List<Class<?>> classes = new ArrayList<Class<?>>();

        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {

            if (clazz.isInterface() || clazz.isAnnotation()) {
                continue;
            }

            boolean oldMatch = oldSet != null && oldSet.classMatch(clazz, matchMethods);
            boolean newMatch = newSet.classMatch(clazz, matchMethods);
            if (isSdef ? (oldMatch || newMatch) : (oldMatch != newMatch)) {
                if (instrumentation.isModifiableClass(clazz)) {
                    classes.add(clazz);
                }
            }
        }

        if (classes.size() > 0) {

            log.info(ZorkaLogger.ZSP_CONFIG, "Retransforming " + classes.size() + " classes.");

            try {
                instrumentation.retransformClasses(classes.toArray(new Class[0]));
            } catch (UnmodifiableClassException e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Error when trying to retransform classes", e);
            }

            return true;
        } else {
            log.info(ZorkaLogger.ZSP_CONFIG, "No classes need to be retransformed.");
        }


        return false;
    }


    @Override
    public boolean isEnabled() {
        return instrumentation != null && instrumentation.isRetransformClassesSupported();
    }

}
