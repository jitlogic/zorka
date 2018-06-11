package com.jitlogic.zorka.core.spy;

public class CachedClassInfo {

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

    public String getClassName() {
        return name;
    }

    public String getSuperclassName() {
        return superclassName;
    }

    public String[] getInterfaceNames() {
        return interfaceNames;
    }

    public boolean isInterface() {
        return 0 != (flags & IS_INTERFACE);
    }
}
