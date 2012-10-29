/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.spy.InstrumentationEngine;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class TestUtil extends ClassLoader {


    public static byte[] readResource(String name) throws Exception {
        InputStream is = TestUtil.class.getResourceAsStream("/"+name);
        byte[] buf = new byte[65536];
        int len = is.read(buf);
        is.close();
        return Arrays.copyOf(buf, len);
    }


    public static Object instantiate(InstrumentationEngine engine, String clazzName) throws Exception {
        String className = clazzName.replace(".", "/");
        byte[] classBytes = readResource(className + ".class");
        byte[] transformed = engine.transform(TestUtil.getSystemClassLoader(), className, null, null, classBytes);

        Class<?> clazz = new TestUtil().defineClass(clazzName, transformed, 0, transformed.length);

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

        if (method != null) {
            if (args.length == 0) {
                return method.invoke(obj);
            } else {
                return method.invoke(obj, args);
            }
        }

        return null;
    }

}
