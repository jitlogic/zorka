/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.test.support;

import com.jitlogic.zorka.agent.AgentInstance;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.spy.SpyClassTransformer;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestUtil extends ClassLoader {

    public static byte[] readResource(String name) throws Exception {
        InputStream is = TestUtil.class.getResourceAsStream("/"+name);
        byte[] buf = new byte[65536];
        int len = is.read(buf);
        is.close();
        byte[] ret = new byte[len];
        System.arraycopy(buf, 0, ret, 0, len);
        return ret;
    }


    public static Object instantiate(SpyClassTransformer engine, String clazzName) throws Exception {
        String className = clazzName.replace(".", "/");
        byte[] classBytes = readResource(className + ".class");
        byte[] transformed = engine.transform(TestUtil.getSystemClassLoader(), className, null, null, classBytes);

        Class<?> clazz = new TestUtil().defineClass(clazzName, transformed, 0, transformed.length);

        return clazz.newInstance();
    }


    public static Object getField(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        Field field = clazz.getField(fieldName);
        boolean accessible = field.isAccessible();

        if (!accessible) {
            field.setAccessible(true);
        }

        Object retVal = field.get(obj);
        field.setAccessible(accessible);

        return retVal;
    }


    public static Object invoke(Object obj, String name, Object...args) throws Exception {
        Method method = null;
        Class<?> clazz = obj.getClass();

        for (Method met : clazz.getMethods()) {
            if (name.equals(met.getName())) {
                method = met;
                break;
            }
        }

        try {

            if (method != null) {
                if (args.length == 0) {
                    return method.invoke(obj);
                } else {
                    return method.invoke(obj, args);
                }
            }
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (Exception e) {
            return e;
        }

        return null;
    }


    public static Object checkForError(Object obj) {
        if (obj instanceof Throwable) {
            System.err.println("Error: " + obj);
            ((Throwable)obj).printStackTrace(System.err);
        }
        return obj;
    }

    public static Object getAttr(String mbsName, String mbeanName, String attr) throws Exception{
        MBeanServerConnection mbs = AgentInstance.getMBeanServerRegistry().lookup(mbsName);
        return mbs.getAttribute(new ObjectName(mbeanName), attr);
    }
}
