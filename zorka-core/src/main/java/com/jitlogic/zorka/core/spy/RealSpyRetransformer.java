/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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


import com.jitlogic.zorka.core.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RealSpyRetransformer implements SpyRetransformer {

    private static final Logger log = LoggerFactory.getLogger(RealSpyRetransformer.class);

    private Instrumentation instrumentation;

    private boolean matchMethods;

    public RealSpyRetransformer(Instrumentation instrumentation, AgentConfig config) {
        this.instrumentation = instrumentation;
        matchMethods = config.boolCfg("zorka.retransform.match.methods", false);
        log.info("Enabling spy retransformer. Full online reconfiguration should be possible.");
    }


    @Override
    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet, boolean isSdef) {
        if (instrumentation == null || !instrumentation.isRetransformClassesSupported()) {
            log.warn("Class retransform is not supported. Skipping.");
            return false;
        }

        if (log.isDebugEnabled() && oldSet != null) {
            for (SpyMatcher m : oldSet.getMatchers()) {
                log.debug("Old matcher: " + m);
            }
        }

        if (log.isDebugEnabled() && newSet != null) {
            for (SpyMatcher m : newSet.getMatchers()) {
                log.debug("New matcher: " + m);
            }
        }
        
        final List<Class<?>> classes = new ArrayList<Class<?>>();

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

        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }

        if (classes.size() > 0) {
            retransform(classes);
            return true;
        } else {
            log.info("No classes need to be retransformed.");
        }

        return false;
    }

    private void retransform(final List<Class<?>> classes) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Class[] cc = new Class[1];

                for (Class c : classes) {
                    if (log.isTraceEnabled()) {
                        log.trace("Retransforming class: " + c.getName());
                    }
                    cc[0] = c;
                    try {
                        instrumentation.retransformClasses(cc);
                        Thread.sleep(100);
                    } catch (Throwable e) {
                        log.error("Error when trying to retransform class:" + c.getName(), e);
                    }
                }

                log.info("Finished retransforming classes.");
            }
        };

        executor.execute(r);
    }

    @Override
    public boolean retransform(Set<String> classNames) {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        for (Class c : instrumentation.getAllLoadedClasses()) {
            if (classNames.contains(c.getName())) {
                classes.add(c);
            }
        }

        if (!classes.isEmpty()) {
            retransform(classes);
            return true;
        }

        return false;
    }

    private Executor executor;

    @Override
    public boolean isEnabled() {
        return instrumentation != null && instrumentation.isRetransformClassesSupported();
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return instrumentation.getAllLoadedClasses();
    }

}
