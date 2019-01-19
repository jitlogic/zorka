/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.zorka.core.spy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class SpyClassLookup {

    private Logger log = LoggerFactory.getLogger(SpyClassLookup.class);

    public static SpyClassLookup INSTANCE = null;

    private Method findLoadedClass;
    private Instrumentation instrumentation;

    public SpyClassLookup() {
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);

            // Populate cache for some basic classes (often referenced)
            if (log.isDebugEnabled()) {
                log.debug("Initialized in reflection mode: " + findLoadedClass);
            }
        } catch (NoSuchMethodException e) {
            log.error("Error obtaining ClassLoader.findLoadedClass reference:", e);
        }
    }

    public SpyClassLookup(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public Class findLoadedClass(ClassLoader loader, String className) {

        if (log.isDebugEnabled()) {
            log.debug("looking for class: " + className);
        }

        if (instrumentation != null) {
            Class[] classes = instrumentation.getAllLoadedClasses();
            if (log.isTraceEnabled()) {
                log.trace("Loaded classes count: " + classes.length);
            }
            for (Class c : classes) {
                if (className.equals(c.getName())) {
                    return c;
                }
            }
            return null;
        }

        if (findLoadedClass != null && loader != null) {
            findLoadedClass.setAccessible(true);
            try {
                return (Class) findLoadedClass.invoke(loader, className);
            } catch (Exception e) {
                log.error("Error looking loaded class: " + className, e);
                return null;
            }
        }

        log.debug("Class " + className + " not resident. Will try via bytecode.");
        return null;
    }
}
