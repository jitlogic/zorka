package com.jitlogic.zorka.util;

import com.jitlogic.zorka.agent.JmxObject;

import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tools for introspection of various objects.
 *
 * TODO fix & describe get() semantics in more detail
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspector {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());


    public Method lookupMethod(Class<?> clazz, String name) {
        try {
            return name != null ? clazz.getMethod(name) : null;
        } catch (NoSuchMethodException e) {
            for (Class<?> icl : clazz.getInterfaces()) {
                Method m = lookupMethod(icl, name);
                if (m != null) {
                    return m;
                }
            }
            Class<?> mcl = clazz;
            while ((mcl = mcl.getSuperclass()) != null && mcl != clazz) {
                Method m = lookupMethod(mcl, name);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }


    public String methodName(String name, String prefix) {
        if (name.startsWith(".")) {
            return null;
        }
        if (name.endsWith("()")) {
            return name.substring(0, name.length()-2);
        } else {
            return prefix + name.substring(0,1).toUpperCase() + name.substring(1);
        }
    }


    public Method lookupGetter(Class<?> clazz, String name) {
        //String methodName
        Method m = lookupMethod(clazz, methodName(name, "get"));
        if (m != null) {
            return m;
        }
        m = lookupMethod(clazz, methodName(name, "is"));
        if (m != null) {
            return m;
        }
        m = lookupMethod(clazz, name);
        return m;
    }



    public Object get(Object obj, Object key) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Map<?, ?>) {
            return ((Map<?,?>)obj).get(key);
        } else if (obj instanceof List<?>) {
            Integer idx = (Integer)ZorkaUtil.coerce(key, Integer.class);
            return idx != null ? ((List<?>)obj).get(idx) : null;
        } else if (obj.getClass().isArray()) {
            Integer idx = (Integer)ZorkaUtil.coerce(key, Integer.class);
            return idx != null ? ((Object[])obj)[idx] : null;
        } else if (obj instanceof CompositeData) {
            return ((CompositeData)obj).get(""+key);
        } else if (obj instanceof Stats){
            return ((Stats)obj).getStatistic(""+key);
        } else if (obj instanceof JmxObject) {
            return ((JmxObject)obj).get(key);
        } else if (obj instanceof Class<?>) {
            String name = (String)key;
            if (name.endsWith("()")) {
                Method m = lookupGetter((Class)obj, name);
                try {
                    return m.invoke(null);
                } catch (Exception e) {
                    log.error("Method '" + m.getName() + "' invocation failed", e);
                    return null;
                }

            }
        }

        if (key instanceof String) {
            String name = (String)key;
            Class<?> clazz = obj.getClass();

            // Try getter method (if any)
            Method m = lookupGetter(clazz, name);
            if (m != null) {
                try {
                    return m.invoke(obj);
                } catch (Exception e) {
                    log.error("Method '" + m.getName() + "' invocation failed", e);
                    return null;
                }
            }

            // Try field (if any)
            try {
                Field field = clazz.getField(name);
                return field.get(name);
            } catch (Exception e) {
                log.error("Field '" + name + "' fetch failed", e);
                return null;
            }
        }

        return null;
    }


    public List<String> listAttrNames(Object obj) {
        List<String> lst = new ArrayList<String>();
        if (obj instanceof Map) {
            for (Object key : ((Map<?,?>)obj).keySet()) {
                lst.add(key.toString());
            }
        } else if (obj instanceof Stats) {
            for (String name : ((Stats)obj).getStatisticNames()) {
                lst.add(name);
            }
        }
        // TODO uzupelnic
        return lst;
    }


}
