package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SpyClassResolver {

    private static final Logger log = LoggerFactory.getLogger(SpyClassResolver.class);

    private Map<String,ClassInfo> cache = new HashMap<String, ClassInfo>();

    public final static String OBJECT_CLAZZ = "java.lang.Object";

    private final static Method findLoadedClass;

    static {
        Method m = null;
        try {
            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            if (!m.isAccessible()) m.setAccessible(true);
        } catch (NoSuchMethodException e) {
            log.error("Error obtaining ClassLoader.findLoadedClass reference.", e);
        }
        findLoadedClass = m;
    }

    private MethodCallStatistic numCalls, classGets;
    private MethodCallStatistic cacheHits, cacheMisses, cacheDels;
    private MethodCallStatistic residentGets, bytecodeGets;

    public SpyClassResolver(MethodCallStatistics stats) {
        numCalls = stats.getMethodCallStatistic("CrvCalls");
        classGets = stats.getMethodCallStatistic("CrvClassGets");
        cacheHits = stats.getMethodCallStatistic("CrvCacheHits");
        cacheMisses = stats.getMethodCallStatistic("CrvCacheMisses");
        cacheDels = stats.getMethodCallStatistic("CrvCacheDeletes");
        residentGets = stats.getMethodCallStatistic("CrvResidentGets");
        bytecodeGets = stats.getMethodCallStatistic("CrvBytecodeGets");
    }

    public String getCommonSuperClass(ClassLoader loader, String type1, String type2) {

        numCalls.logCall();

        ClassInfo ci1 = getClassInfo(loader, type1), ci2 = getClassInfo(loader, type2);

        ClassInfo rslt = null;

        if (!ci1.isInterface() && !ci2.isInterface()) {
            // Both are classes

            List<ClassInfo> cs1 = getAllSuperclasses(loader, ci1), cs2 = getAllSuperclasses(loader, ci2);
            cs1.add(ci1); cs2.add(ci2);

            int csl = Math.min(cs1.size(), cs2.size());

            for (int i = 0; i < csl; i++) {
                if (cs1.get(i).getClassName().equals(cs2.get(i).getClassName())) {
                    rslt = cs1.get(i);
                }
            }
        } else if (ci1.isInterface() && ci2.isInterface()) {
            return OBJECT_CLAZZ;
        } else {
            ClassInfo ci = ci1.isInterface() ? ci1 : ci2, co = ci1.isInterface() ? ci2 : ci1;

            while (!OBJECT_CLAZZ.equals(co.getClassName())) {
                if (interfaceMatches(loader, ci, co)) {
                    return ci.getClassName();
                }
                co = getClassInfo(loader, co.getSuperclassName());
            }

            return OBJECT_CLAZZ;
        }

        return rslt != null ? rslt.getClassName() : OBJECT_CLAZZ;
    }

    private boolean interfaceMatches(ClassLoader loader, ClassInfo ci, ClassInfo cc) {
        String[] ifcs = cc.getInterfaceNames();
        if (ifcs != null) {
            // Check for direct interface implementation
            for (String ifc : ifcs) {
                if (ci.getClassName().equals(ifc)) {
                    return true;
                }
            }
            // Check for indirect interface implementation
            for (String ifc : ifcs) {
                ClassInfo ci1 = getClassInfo(loader, ifc);
                if (ci1 != null && interfaceMatches(loader, ci, ci1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<ClassInfo> getAllSuperclasses(ClassLoader loader, String type) {
        ClassInfo ci = getClassInfo(loader, type);
        return ci != null ? getAllSuperclasses(loader, ci) : null;
    }


    private List<ClassInfo> getAllSuperclasses(ClassLoader loader, ClassInfo ci) {
        List<ClassInfo> rslt = new ArrayList<ClassInfo>();

        ClassInfo i = ci;

        do {
            i = getClassInfo(loader, i.getSuperclassName());
            rslt.add(i);
        } while (!"java.lang.Object".equals(i.getClassName()));

        Collections.reverse(rslt);

        return rslt;
    }


    public ClassInfo getClassInfo(ClassLoader loader, String type) {
        classGets.logCall(1);

        ClassInfo rslt = getCached(type);

        if (rslt != null) {
            cacheHits.logCall();
        }

        try {
            // In case, application code made this method inaccessible again ...
            if (!findLoadedClass.isAccessible()) findLoadedClass.setAccessible(true);

            Class<?> clazz = (Class)findLoadedClass.invoke(loader, type);
            if (clazz != null) {
                if (rslt != null) cacheDels.logCall();
                rslt = new ResidentClassInfo(clazz);
                delCached(type);
                residentGets.logCall();
                return rslt;
            }
        } catch (IllegalAccessException e) {
            log.error("Error calling findLoadedClass(): ", e);
        } catch (InvocationTargetException e) {
            log.error("Error calling findLoadedClass(): ", e);
        }

        if (rslt != null) {
            return rslt;
        } else {
            cacheMisses.logCall();
        }

        InputStream is = loader.getResourceAsStream(type.replace(".", "/") + ".class");
        byte[] classBytes = null;
        if (is != null) {
            try {
                classBytes = ZorkaUtil.slurp(is);
            } catch (Exception e) {
                log.error("Error reading class bytecode: ", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Cannot close stream: ", e);
                }
            }
        } else {
            log.error("Bytecode for class not found: " + type + " (using class loader: " + loader + ")");
        }

        if (classBytes == null) return null;

        ClassReader reader = new ClassReader(classBytes);
        String[] ifcs = reader.getInterfaces();

        for (int i = 0; i < ifcs.length; i++) {
            ifcs[i] = ifcs[i].replace('/', '.').intern();
        }

        rslt = new CachedClassInfo(
            0 != (0x00000200 & reader.getAccess()) ? CachedClassInfo.IS_INTERFACE : 0,
            reader.getClassName().replace('/', '.').intern(),
            reader.getSuperName().replace('/', '.').intern(),
            ifcs);

        setCached(type, rslt);
        bytecodeGets.logCall();

        return rslt;

    }

    private synchronized ClassInfo getCached(String type) {
        return cache.get(type);
    }

    private synchronized void setCached(String type, ClassInfo ci) {
        cache.put(type, ci);
    }

    private synchronized void delCached(String type) {
        cache.remove(type);
    }

    public synchronized int getCacheSize() {
        return cache.size();
    }
}
