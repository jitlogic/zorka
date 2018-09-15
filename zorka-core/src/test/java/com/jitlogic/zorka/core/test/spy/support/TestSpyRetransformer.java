package com.jitlogic.zorka.core.test.spy.support;

import com.jitlogic.zorka.core.spy.SpyMatcherSet;
import com.jitlogic.zorka.core.spy.SpyRetransformer;

import java.util.HashSet;
import java.util.Set;

public class TestSpyRetransformer implements SpyRetransformer {

    private Set<String> classNames = new HashSet<String>();

    public static boolean enabled = false;

    @Override
    public boolean retransform(SpyMatcherSet oldSet, SpyMatcherSet newSet, boolean isSdef) {
        return enabled;
    }

    @Override
    public boolean retransform(Set<String> classNames) {
        this.classNames.addAll(classNames);
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return new Class[0];
    }

    public Set<String> getClassNames() {
        return classNames;
    }
}
