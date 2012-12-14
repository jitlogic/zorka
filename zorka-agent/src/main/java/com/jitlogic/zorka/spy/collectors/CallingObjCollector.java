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

package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CallingObjCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private Object nsObj = null;
    private String name;

    public CallingObjCollector(Object nsObj, String name) {
        this.nsObj = nsObj;
        this.name = name;
    }


    public SpyRecord process(int stage, SpyRecord record) {
        if (nsObj != null) {
            // TODO better lookup here (for overloaded methods)
            Method method = null;

            try {
                method = nsObj.getClass().getMethod(name, SpyRecord.class);
            } catch (NoSuchMethodException e) {
            }

            try {
                if (method == null)
                    method = nsObj.getClass().getMethod(name, Object.class);
            } catch (NoSuchMethodException e) {
                log.error("Passed object " + nsObj + " has no suitable method named '" + name + "'");
            }


            if (method != null) {
                try {
                    method.invoke(nsObj, record);
                } catch (IllegalAccessException e) {
                    log.error("Error calling method " + name + " of " + nsObj, e);
                } catch (InvocationTargetException e) {
                    log.error("Error calling method " + name + " of " + nsObj, e);
                }
            }
        }

        return record;
    }
}
