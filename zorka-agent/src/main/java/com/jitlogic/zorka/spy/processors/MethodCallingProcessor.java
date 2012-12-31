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
package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MethodCallingProcessor implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private String src, dst;
    private String methodName;
    private Object[] args;
    private Class<?>[] argTypes;


    public MethodCallingProcessor(String src, String dst, String methodName, Object... args) {
        this.src = src; this.dst = dst;
        this.methodName = methodName;
        this.args = args;

        argTypes = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] != null ? args[i].getClass() : null;
        }
    }


    public SpyRecord process(SpyRecord record) {
        Object val = record.get(src);

        if (val == null) {
            return record;
        }

        Method method = lookupMethod(val.getClass(), methodName, argTypes);

        if (method == null) {
            return record;
        }

        try {
            val = method.invoke(val, args);
            record.put(dst, val);
        } catch (Exception e) {
            log.error("Error processing record calling its method", e);
        }

        return record;
    }

    // TODO move this method to ObjectInspector
    public Method lookupMethod(Class<?> clazz, String methodName, Class<?>...argTypes) {
        for (Method m : clazz.getMethods()) {
            if (methodName.equals(m.getName())) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (argTypes.length == 0 && paramTypes.length == 0) {
                    return m;
                }

                if (argTypes.length != paramTypes.length) {
                    continue;
                }

                boolean matches = true;

                for (int i = 0; i < argTypes.length; i++) {
                    if (argTypes[i] == null && paramTypes[i].isPrimitive()) {
                        matches = false; break;
                    }

                    if (paramTypes[i].isPrimitive()) {
                        paramTypes[i] = primitives.get(paramTypes[i].getName());
                    }

                    if (!paramTypes[i].isAssignableFrom(argTypes[i])) {
                        matches = false; break;
                    }
                }

                if (matches) {
                    return m;
                }
            }
        }

        return null;
    }

    private static Map<String,Class<?>> primitives;

    static {
        primitives = new HashMap<String, Class<?>>(32);
        primitives.put("int", Integer.class);
        primitives = Collections.unmodifiableMap(primitives);
    }
}
