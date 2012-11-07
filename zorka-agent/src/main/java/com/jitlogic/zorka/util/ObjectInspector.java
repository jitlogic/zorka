package com.jitlogic.zorka.util;

import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.mbeans.ZorkaStats;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools for introspection of various objects.
 *
 * TODO fix & describe get() semantics in more detail
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class ObjectInspector {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    // TODO multiargument get(), eg. getAll(Object obj, Object...path)  - there are several places it would be useful

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
        } else if (obj instanceof TabularData) {
            String[] keys = key.toString().split("\\,");
            // TODO coerce keys to proper data types
            obj = ((TabularData)obj).get(keys);
        } else if (obj instanceof ZorkaStats) {
            return ((ZorkaStats)obj).getStatistic(key.toString());
        } else if (ZorkaUtil.instanceOf(obj.getClass(), "javax.management.j2ee.statistics.Stats")) {
            try {
                Method m = obj.getClass().getMethod("getStatistic", String.class);
                if (m != null) {
                    return m.invoke(obj, key);
                }
            } catch (Exception e) {
                log.error("Error invoking getStatistic('" + key + "')", e);
            }
        } else if (obj instanceof JmxObject) {
            return ((JmxObject)obj).get(key);
        }

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


    /**
     * Lists attributes of an object. Depending on object type,
     * @param obj
     * @return
     */
    public List<?> list(Object obj) {
        List<String> lst = new ArrayList<String>();
        if (obj instanceof Map) {
            for (Object key : ((Map<?,?>)obj).keySet()) {
                lst.add(key.toString());
            }
        } else if (obj instanceof List<?>) {
            int len = ((List)obj).size();
            List<Integer> ret = new ArrayList<Integer>(len);
            for (int i = 0; i < len; i++) {
                ret.add(i);
            }
            return ret;
        } else if (obj.getClass().isArray()) {
            int len = ((Object[])obj).length;
            List<Integer> ret = new ArrayList<Integer>(len);
            for (int i = 0; i < len; i++) {
                ret.add(i);
            }
            return ret;
        } else if (obj instanceof CompositeData) {
            for (Object s : ((CompositeData)obj).getCompositeType().keySet()) {
                lst.add(s.toString());
            }
        } else if (obj instanceof TabularData) {
            for (Object k : ((TabularData)obj).keySet()) {
                lst.add(ZorkaUtil.join(",", (Collection<?>)k));
            }
        } else if (obj instanceof ZorkaStats) {
            lst = Arrays.asList(((ZorkaStats)obj).getStatisticNames());
        } else if (ZorkaUtil.instanceOf(obj.getClass(), "javax.management.j2ee.statistics.Stats")) {
            try {
                Method m = obj.getClass().getMethod("getStatisticNames");
                if (m != null) {
                    return Arrays.asList((Object[])m.invoke(obj));
                }
            } catch (Exception e) {
                log.error("Error invoking getStatisticNames()", e);
            }
        } else if (obj instanceof JmxObject) {
            try {
                MBeanInfo mbi = ((JmxObject) obj).getConn().getMBeanInfo(((JmxObject) obj).getName());
                for (MBeanAttributeInfo mba : mbi.getAttributes()) {
                    lst.add(mba.getName());
                }
            } catch (Exception e) {
                log.error("Error fetching object attributes.");
            }
        }

        Collections.sort(lst);

        return lst;
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

    @SuppressWarnings("unchecked")
    public Set<ObjectName> queryNames(MBeanServerConnection conn, String query) {
        try {
            ObjectName on = new ObjectName(query);
            return (Set<ObjectName>)conn.queryNames(on, null);
        } catch (Exception e) {
            //log.error("Error resolving object names.", e);
            return new HashSet<ObjectName>();
        }
    }

    private Pattern pattern = Pattern.compile("\\$\\{([^\\}]+)\\}");

    public String substitute(String input, Object[] vals) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String expr = m.group(1);
            String[] segs = expr.split("\\.");
            Object v = vals[Integer.parseInt(segs[0])];
            for (int i = 1; i < segs.length; i++) {
                v = get(v, segs[i]);
            }
            m.appendReplacement(sb, ""+v);
        }

        m.appendTail(sb);

        return sb.toString();
    }

}
