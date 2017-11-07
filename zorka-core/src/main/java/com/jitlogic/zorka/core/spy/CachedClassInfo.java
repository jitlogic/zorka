package com.jitlogic.zorka.core.spy;

public class CachedClassInfo implements ClassInfo {

    final static int IS_INTERFACE = 0x01;

    private final int flags;
    private final String name, superclassName;
    private final String[] interfaceNames;

    CachedClassInfo(int flags, String name, String superclassName, String[] interfaceNames) {
        this.name = name;
        this.superclassName = superclassName;
        this.interfaceNames = interfaceNames;
        this.flags = flags;
    }

    @Override
    public String getClassName() {
        return name;
    }

    @Override
    public String getSuperclassName() {
        return superclassName;
    }

    @Override
    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    @Override
    public boolean isInterface() {
        return 0 != (flags & IS_INTERFACE);
    }
}
