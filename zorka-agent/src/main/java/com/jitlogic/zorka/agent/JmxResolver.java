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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;

import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

public class JmxResolver {
	
	private static final ZorkaLogger log = ZorkaLogger.getLogger(JmxResolver.class); 
	
	@SuppressWarnings("unchecked")
	public Set<ObjectName> queryNames(MBeanServerConnection conn, String query) {
		try {
			ObjectName on = new ObjectName(query);
			return (Set<ObjectName>)conn.queryNames(on, null);
		} catch (Exception e) {
			log.error("Error resolving object names.", e);
			return new HashSet<ObjectName>();
		}
	}

	// TODO przenieść do dedykowanego obiektu i zrefaktorować
	public static Object get(Object obj, Object key) {
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
				ZorkaUtil.log.error("Field '" + name + "' fetch failed", e);
				return null;
			}
		}
			
		return null;
	}
	
}
