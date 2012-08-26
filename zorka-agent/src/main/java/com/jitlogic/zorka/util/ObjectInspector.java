package com.jitlogic.zorka.util;

import com.jitlogic.zorka.agent.JmxObject;

import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Tools for introspection of various objects.
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspector {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

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
            Method m = ZorkaUtil.lookupGetter(clazz, name);
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

}
