/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
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

import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SpyClassResolver {

    private static final Logger log = LoggerFactory.getLogger(SpyClassResolver.class);

    private Map<String,CachedClassInfo> cache = new HashMap<String, CachedClassInfo>();

    public final static String OBJECT_CLAZZ = "java.lang.Object";

    private MethodCallStatistic numCalls, classGets;
    private MethodCallStatistic cacheHits, cacheMisses;
    private MethodCallStatistic residentGets, bytecodeGets;

    public SpyClassResolver(MethodCallStatistics stats) {
        numCalls = stats.getMethodCallStatistic("CrvCalls");
        classGets = stats.getMethodCallStatistic("CrvClassGets");
        cacheHits = stats.getMethodCallStatistic("CrvCacheHits");
        cacheMisses = stats.getMethodCallStatistic("CrvCacheMisses");
        residentGets = stats.getMethodCallStatistic("CrvResidentGets");
        bytecodeGets = stats.getMethodCallStatistic("CrvBytecodeGets");
    }

    public String getCommonSuperClass(ClassLoader loader, String type1, String type2) {

        numCalls.logCall();

        CachedClassInfo ci1 = getClassInfo(loader, type1), ci2 = getClassInfo(loader, type2);

        CachedClassInfo rslt = null;

        if (!ci1.isInterface() && !ci2.isInterface()) {
            // Both are classes

            List<CachedClassInfo> cs1 = getAllSuperclasses(loader, ci1), cs2 = getAllSuperclasses(loader, ci2);
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
            CachedClassInfo ci = ci1.isInterface() ? ci1 : ci2, co = ci1.isInterface() ? ci2 : ci1;

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

    private boolean interfaceMatches(ClassLoader loader, CachedClassInfo ci, CachedClassInfo cc) {
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
                CachedClassInfo ci1 = getClassInfo(loader, ifc);
                if (ci1 != null && interfaceMatches(loader, ci, ci1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<CachedClassInfo> getAllSuperclasses(ClassLoader loader, String type) {
        CachedClassInfo ci = getClassInfo(loader, type);
        return ci != null ? getAllSuperclasses(loader, ci) : null;
    }


    private List<CachedClassInfo> getAllSuperclasses(ClassLoader loader, CachedClassInfo ci) {
        List<CachedClassInfo> rslt = new ArrayList<CachedClassInfo>();

        CachedClassInfo i = ci;

        do {
            i = getClassInfo(loader, i.getSuperclassName());
            rslt.add(i);
        } while (!"java.lang.Object".equals(i.getClassName()));

        Collections.reverse(rslt);

        return rslt;
    }


    public CachedClassInfo getClassInfo(ClassLoader loader, String type) {

        if (log.isTraceEnabled()) {
            log.trace("Class: " + type + ", cached: " + cache.size());
        }

        classGets.logCall(1);

        CachedClassInfo rslt = getCached(type);

        if (rslt != null) {
            cacheHits.logCall();
            return rslt;
        }

        cacheMisses.logCall();

        Class<?> clazz = SpyClassLookup.INSTANCE.findLoadedClass(loader, type);
        if (clazz != null) {
            rslt = new CachedClassInfo(
                    clazz.isInterface() ? CachedClassInfo.IS_INTERFACE : 0,
                    clazz.getName(),
                    clazz.getSuperclass() != null ? clazz.getSuperclass().getName() : null,
                    clazz.getInterfaces() != null ? new String[clazz.getInterfaces().length] : new String[0]);
            setCached(type, rslt);
            residentGets.logCall();
            return rslt;
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

    private synchronized CachedClassInfo getCached(String type) {
        return cache.get(type);
    }

    private synchronized void setCached(String type, CachedClassInfo ci) {
        cache.put(type, ci);
    }

    private synchronized void delCached(String type) {
        cache.remove(type);
    }

    public synchronized int getCacheSize() {
        return cache.size();
    }
}
