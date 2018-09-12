package com.jitlogic.zorka.core.spy;

import java.util.List;

public interface SpyMatcherSet {
    /**
     * Checks whenever class name matches any of included matchers. If match result is undecided
     * (eg. some matchers use interface or class annotations), also returns true.
     * <p/>
     * This method is used by class transformer to decide whenever
     *
     * @param className
     * @return
     */
    boolean classMatch(String className);

    boolean classMatch(Class<?> clazz, boolean matchMethods);

    /**
     * Checks whetner given method matches any of included matchers. If passed method annotations list is null
     * and any of included matchers looks for mathod annotations, this method will return true.
     *
     * @param className         class name
     * @param superclasses      superclass name
     * @param classAnnotations  list of class names of class annotations
     * @param classInterfaces   list of class names of interfaces directly implemented by this class
     * @param access            method access flags
     * @param methodName        method name
     * @param methodSignature   method signature string
     * @param methodAnnotations list of class names of method annotations
     * @return true if method matches
     */
    boolean methodMatch(String className, List<String> superclasses,
                        List<String> classAnnotations, List<String> classInterfaces,
                        int access, String methodName,
                        String methodSignature, List<String> methodAnnotations);

    void clear();

    List<SpyMatcher> getMatchers();
}
