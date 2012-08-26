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
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspector {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());


    public Method lookupMethod(Class<?> clazz, String name) {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            for (Class<?> icl : clazz.getInterfaces()) {
                Method m = lookupMethod(icl, name);
                if (m != null) {
                    return m;
                }
            }
            Class<?> mcl = clazz;
            while ((mcl = clazz.getSuperclass()) != null) {
                Method m = lookupMethod(mcl, name);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }


    public Method lookupGetter(Class<?> clazz, String name) {
        Method m = lookupMethod(clazz, "get" + name.substring(0,1).toUpperCase() + name.substring(1));
        if (m != null) {
            return m;
        }
        m = lookupMethod(clazz, "is" + name.substring(0, 1) + name.substring(1));
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
