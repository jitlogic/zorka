package com.jitlogic.zorka.util;

import com.jitlogic.zorka.agent.JmxObject;

import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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


    /**
     * Gets (logical) attribute from an object. Exact semantics may differ
     * depending on type of object. If object is an class, get() will look
     * in its static methods/fields. Instance metods or fields will be used
     * otherwise.
     *
     * @param obj object subjected
     *
     * @param key attribute identified (name, index, etc. - depending on object type)
     *
     * @return attribute value or null if no matching attribute has been found
     */
    public Object get(Object obj, Object key) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = (obj instanceof Class<?>) ? (Class)obj : obj.getClass();

        if (key instanceof String && key.toString().endsWith("()")) {
            // Explicit method call for attributes ending with '()'
            String name = key.toString();
            return fetchViaMethod(obj, clazz, lookupMethod(clazz, name.substring(0, name.length()-2)));
        }

        if (key instanceof String && key.toString().startsWith(".")) {
            // Explicit field accesses for attributes starting with '.'
            return fetchFieldVal(obj, clazz, key.toString().substring(1));
        }

        if (obj instanceof Map<?, ?>) {
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
            // TODO cut off j2ee dependency - use reflection;
            return ((Stats)obj).getStatistic(""+key);
        } else if (obj instanceof JmxObject) {
            return ((JmxObject)obj).get(key);
        }  // TODO support for tabular data

        if (key instanceof String) {
            String name = (String) key;

            Method method = lookupMethod(clazz, "get" + name.substring(0, 1).toUpperCase() + name.substring(1));

            if (method == null) {
                method = lookupMethod(clazz, "is" + name.substring(0, 1).toUpperCase() + name.substring(1));
            }

            if (method == null) {
                method = lookupMethod(clazz, name);
            }

            if (method != null) {
                return fetchViaMethod(obj, clazz, method);
            }

            return fetchFieldVal(obj, clazz, name);
        }

        return null;
    }

    private Object fetchViaMethod(Object obj, Class<?> clazz, Method method) {
        if (method != null) {
            try {
                return method.invoke(obj);
            } catch (Exception e) {
                log.error("Method '" + method.getName() + "' invocation failed", e);
                return null;
            }
        }
        return null;
    }

    private Object fetchFieldVal(Object obj, Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name.startsWith(".") ? name.substring(1) : name);
            boolean accessible = field.isAccessible();
            if (!accessible) field.setAccessible(true);
            Object ret = field.get(obj);
            if (!accessible) field.setAccessible(accessible);
            return ret;
        } catch (Exception e) {
            log.error("Field '" + name + "' fetch failed", e);
            return null;
        }
    }


    public List<String> list(Object obj) {
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

        Collections.sort(lst);

        return lst;
    }


}
