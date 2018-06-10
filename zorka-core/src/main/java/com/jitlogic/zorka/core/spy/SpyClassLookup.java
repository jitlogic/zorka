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

        log.warn("Failed looking for class " + className + ": no instrumentation nor reflection/loader available.");
        return null;
    }
}
