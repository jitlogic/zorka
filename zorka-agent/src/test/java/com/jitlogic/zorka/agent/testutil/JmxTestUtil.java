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

package com.jitlogic.zorka.agent.testutil;

import com.jitlogic.zorka.agent.AgentGlobals;
import com.jitlogic.zorka.agent.JavaAgent;
import com.jitlogic.zorka.agent.JmxObject;
import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.spy.InstrumentationEngine;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class JmxTestUtil extends ClassLoader {

    private MBeanServer mbs;
    private ZorkaBshAgent agent;


    public void setUp(ZorkaBshAgent agent) {
        mbs = new MBeanServerBuilder().newMBeanServer("test", null, null);
        this.agent = agent;
        this.agent.getMBeanServerRegistry().register("test", mbs, null);
    }


    public void tearDown() {
        agent.getMBeanServerRegistry().unregister("test");
    }


    public TestJmx makeTestJmx(String name, long nom, long div) throws Exception {
        TestJmx bean = new TestJmx();
        bean.setNom(nom); bean.setDiv(div);

        mbs.registerMBean(bean, new ObjectName(name));

        return bean;
    }

    public static byte[] readResource(String name) throws Exception {
        InputStream is = JmxTestUtil.class.getResourceAsStream("/"+name);
        byte[] buf = new byte[65536];
        int len = is.read(buf);
        is.close();
        return Arrays.copyOf(buf, len);
    }


    public static Object instantiate(InstrumentationEngine engine, String clazzName) throws Exception {
        String className = clazzName.replace(".", "/");
        byte[] classBytes = readResource(className + ".class");
        byte[] transformed = engine.transform(JmxTestUtil.getSystemClassLoader(), className, null, null, classBytes);

        Class<?> clazz = new JmxTestUtil().defineClass(clazzName, transformed, 0, transformed.length);

        return clazz.newInstance();
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
        } catch (Throwable e) {
            return e;
        }

        return null;
    }


    public static void checkForError(Object obj) {
        if (obj instanceof Throwable) {
            System.err.println("Error: " + obj);
            ((Throwable)obj).printStackTrace(System.err);
        }
    }

    public static Object getAttr(String mbsName, String mbeanName, String attr) throws Exception{
        MBeanServerConnection mbs = AgentGlobals.getMBeanServerRegistry().lookup(mbsName);
        return mbs.getAttribute(new ObjectName(mbeanName), attr);
    }
}
