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

package com.jitlogic.zorka.agent;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.jitlogic.zorka.integ.ZorkaLog;
import com.jitlogic.zorka.integ.ZorkaLogger;

public class JmxObject {
	
	private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

	private final ObjectName name;
	private final MBeanServerConnection conn;
    private final ClassLoader classLoader;

    public JmxObject(ObjectName name, MBeanServerConnection conn, ClassLoader classLoader) {
		this.name = name;
		this.conn = conn;
        this.classLoader = classLoader;
	}
	
	public Object get(Object key) {
        ClassLoader cl0 = null;
        Object ret = null;
		try {
            if (classLoader != null) {
                cl0 = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(classLoader);
            }
			ret = conn.getAttribute(name, key.toString());
		} catch (Exception e) {
			log.error("Cannot get attribute '" + key + "' of '" + name + "'", e);
		} finally {
            if (cl0 != null) {
                Thread.currentThread().setContextClassLoader(cl0);
            }
        }

        return ret;
	}
	
	public ObjectName getName() {
		return name;
	}
	
	public MBeanServerConnection getConn() {
		return conn;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JmxObject &&
			name.equals(((JmxObject)obj).name) &&
			conn.equals(((JmxObject)obj).conn);
	}

	@Override
	public int hashCode() {
		return 17 * name.hashCode() + 31 * conn.hashCode();
	}
	
	@Override
	public String toString() {
		return name.toString();
	}
}
