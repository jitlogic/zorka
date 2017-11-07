package com.jitlogic.zorka.core.spy;

public interface ClassInfo {

    String getClassName();

    String getSuperclassName();

    String[] getInterfaceNames();

    boolean isInterface();
}
