package com.jitlogic.zorka.core.spy;

public class ResidentClassInfo implements ClassInfo {

    private Class<?> clazz;

    ResidentClassInfo(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getClassName() {
        return clazz.getName();
    }

    @Override
    public String getSuperclassName() {
        return clazz.getSuperclass().getName();
    }

    @Override
    public String[] getInterfaceNames() {
        Class<?>[] interfaces = clazz.getInterfaces();
        String[] rslt = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            rslt[i] = interfaces[i].getName();
        }
        return rslt;
    }

    @Override
    public boolean isInterface() {
        return 0 != (0x00000200 & clazz.getModifiers());
    }

}
