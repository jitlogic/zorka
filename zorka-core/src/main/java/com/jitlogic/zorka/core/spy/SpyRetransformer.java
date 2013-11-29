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
package com.jitlogic.zorka.core.spy;


import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SpyRetransformer {

    private static final ZorkaLog log = ZorkaLogger.getLog(SpyRetransformer.class);

    private Instrumentation instrumentation;

    private Method retransformMethod;
    private Method isModifiable;

    public SpyRetransformer(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        if (instrumentation != null) {
            checkRetransformCapability();
        }
    }

    private void checkRetransformCapability() {
        Method isRetransformSupported = ObjectInspector.lookupMethod(instrumentation.getClass(), "isRetransformClassesSupported");
        if (isRetransformSupported != null) {
            try {
                if ((Boolean) isRetransformSupported.invoke(instrumentation)) {
                    log.info(ZorkaLogger.ZSP_CONFIG, "Retransform is supported on this platform.");
                    isModifiable = ObjectInspector.lookupMethod(instrumentation.getClass(), "isModifiableClass");
                    retransformMethod = ObjectInspector.lookupMethod(instrumentation.getClass(), "retransformClasses");
                } else {
                    log.info(ZorkaLogger.ZSP_CONFIG,
                            "Retransform is not supported on this platform. Online reconfiguration will be severely crippled.");
                }
            } catch (IllegalAccessException e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Cannot determine retransform capability", e);
            } catch (InvocationTargetException e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Cannot determine retransform capability", e);
            }
        } else {
            log.info(ZorkaLogger.ZSP_CONFIG,
                    "Retransform is not supported on this platform. Online reconfiguration will be severely crippled.");

        }
    }


    protected boolean isModifiable(Class<?> clazz) {
        if (isModifiable != null && clazz != null) {
            try {
                return (Boolean) isModifiable.invoke(instrumentation, clazz);
            } catch (IllegalAccessException e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Cannot determine class " + clazz.getName() + " modifiability", e);
            } catch (InvocationTargetException e) {
                log.error(ZorkaLogger.ZSP_CONFIG, "Cannot determine class " + clazz.getName() + " modifiability", e);
            }
        }
        return false;
    }


    protected void retransformClasses(Class<?>[] classes) {
        try {
            log.info(ZorkaLogger.ZSP_DEBUG, "Retransforming " + classes.length + " classes.");
            retransformMethod.invoke(instrumentation, classes);
        } catch (IllegalAccessException e) {
            log.error(ZorkaLogger.ZSP_ERRORS, "Cannot call instrumentation retransform method", e);
        } catch (InvocationTargetException e) {
            log.error(ZorkaLogger.ZSP_ERRORS, "Cannot call instrumentation retransform method", e);
        }
    }


    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet) {
        if (retransformMethod == null || isModifiable == null) {
            return false;
        }

        List<Class<?>> classes = new ArrayList<Class<?>>();

        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            String clazzName = clazz.getName();
            if ((oldSet != null && oldSet.classMatch(clazzName)) || newSet.classMatch(clazzName)) {
                if (isModifiable(clazz)) {
                    classes.add(clazz);
                }
            }
        }

        if (classes.size() > 0) {
            retransformClasses(classes.toArray(new Class[0]));
        }

        return false;
    }


    public boolean isEnabled() {
        return retransformMethod != null;
    }

}
