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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;


public class ObjectDumper {

	private static final Map<String,Integer> filteredClasses;
	private static final Map<String,Integer> filteredMethods;
	
	private static final int PRINT = 1;
	private static final int OMIT = 2;
	
	private static final String LEAD = "    ";
	private static final int MAX_DEPTH = 8;
	private static final int SCREEN_WIDTH = 120;
	
	
	public ObjectDumper() {
	}
	
	public String dump(Object obj) {
		StringBuilder sb = new StringBuilder();
		serialize("", obj, sb, 0);
		return sb.toString();
	}
	
	private void serialize(String lead, Object obj, StringBuilder sb, int depth) {
		
		if (obj == null) {
			sb.append("null");
			return;
		}
		
		Integer c = filteredClasses.get(obj.getClass().getName());
		
		if (c != null) {
			if (c == PRINT) {
				sb.append(obj);
				return;
			}
		
			if (c == OMIT) { return; }
		}

		if (depth > MAX_DEPTH) {
			sb.append("<...>");
			return;
		}
		
		if (obj instanceof JmxObject) {
			serializeJmxObject(lead, obj, sb, depth); 
		} else if (obj instanceof Collection) {
			serializeCollection(lead, obj, sb, depth);
		} else if (obj instanceof Map) {
			serializeMap(lead, obj, sb, depth); 
		} else if (obj instanceof Stats) {
			serializeStats(lead, obj, sb, depth);
		} else if (obj instanceof CompositeData) {
			serializeCompositeData(lead, obj, sb, depth);
		} else if (obj instanceof TabularData) {
			serializeTabularData(lead, obj, sb, depth);
		} else {
			serializePojoObj(lead, obj, sb, depth);
		}
	} //serialize()
	
	
	private void serializePojoObj(String lead, Object obj, StringBuilder sb,
			int depth) {
		sb.append("\n");
		for (Method m : obj.getClass().getMethods()) {
			String name = m.getName();
			if (m.getParameterTypes().length != 0) { continue; }
			if (!name.startsWith("get") && !name.startsWith("is")) { continue; }
			Integer cm = filteredMethods.get(name);
			if (cm != null && cm.equals(OMIT)) { continue; }
			name = name.startsWith("get") ? name.substring(3) : name.substring(2);
			name = name.substring(0,1).toLowerCase() + name.substring(1);
			sb.append(lead); sb.append(name); sb.append(" : "); 
			sb.append(m.getReturnType().getName()); sb.append(" = ");
			try {
				Object o = m.invoke(obj);
				serialize(lead+LEAD, o, sb, depth+1);
			} catch (Exception e) {
				sb.append("<error: " + e.getMessage() + ">");
			}
		}
	}
	
	
	private void serializeTabularData(String lead, Object obj,
			StringBuilder sb, int depth) {
		TabularData td = (TabularData)obj;
		for (Object ksObj : td.keySet()) {
			sb.append(lead);
			serialize(lead+LEAD, ksObj, sb, depth+1);
			sb.append("\n");
		}
	}
	
	
	private void serializeCompositeData(String lead, Object obj,
			StringBuilder sb, int depth) {
		CompositeData data = (CompositeData)obj;
		CompositeType type = data.getCompositeType();
		sb.append("\n");
		for (Object o : type.keySet()) {
			String name = o.toString();
			Object value = data.get(name);
			sb.append(lead); sb.append(name); sb.append(" : ");
			if (value != null) {
				sb.append(value.getClass().getName());
				sb.append(" = ");
				serialize(lead+LEAD, value, sb, depth+1);
			} else {
				sb.append("(?) = <null>");
			}
			sb.append("\n");
		}
	}
	
	
	private void serializeStats(String lead, Object obj, StringBuilder sb,
			int depth) {
		Stats stats = (Stats)obj;
		for (Statistic s : stats.getStatistics()) {
			sb.append(lead); sb.append(s.getName()); sb.append(" : "); 
			sb.append(s.getClass().getName()); sb.append(" = ");
			serialize(lead+LEAD, s, sb, depth+1);
		}
	}
	
	
	private void serializeMap(String lead, Object obj, StringBuilder sb,
			int depth) {
		Map<?,?> map = (Map<?,?>)obj;
		sb.append("{");
		int pos = sb.length();
		for (Entry<?, ?> e : map.entrySet()) {
			sb.append(e.getKey());
			sb.append(" : ");
			serialize(lead+LEAD, e.getValue(), sb, depth+1);
			pos = checkNewLine(lead, sb, pos);				
		}
		sb.append("}");
	}
	
	
	private void serializeCollection(String lead, Object obj, StringBuilder sb,
			int depth) {
		Collection<?> col = (Collection<?>)obj;
		sb.append("[");
		int pos = sb.length();
		for (Object o : col) {
			serialize(lead+LEAD, o, sb, depth+1);
			sb.append(",");
			pos = checkNewLine(lead, sb, pos);
		}
		sb.append("]");
	}
	
	
	private void serializeJmxObject(String lead, Object obj, StringBuilder sb,
			int depth) {
		JmxObject jmx = (JmxObject)obj;
		sb.append(jmx.getName()); sb.append(":\n");
		try {
			MBeanInfo mi = jmx.getConn().getMBeanInfo(jmx.getName());
			for (MBeanAttributeInfo mbi : mi.getAttributes()) {
				try {
					Object o = jmx.getConn().getAttribute(jmx.getName(), mbi.getName());
					sb.append(lead); sb.append(mbi.getName()); sb.append(" : ");
					sb.append(mbi.getType()); sb.append(" = ");
					if (o != obj) {
						serialize(lead+LEAD, o, sb, depth+1);
					} else {
						sb.append("<points to itself>");
					}
					sb.append("\n");
				} catch (Exception e) {
					sb.append("<error: " + e.getMessage() + ">");
				}
			}
		} catch (Exception e) {
			sb.append("<error: " + e.getMessage() + ">");
		}
	}
	
	
	private int checkNewLine(String lead, StringBuilder sb, int pos) {
		if (sb.length()-pos > SCREEN_WIDTH) {
			sb.append("\n");
			int rpos = sb.length();
			sb.append(lead);
			return rpos;
		}
		return pos;
	}

	
	static {
		Map<String,Integer> fcs = new HashMap<String, Integer>();
		
		String[] simpleTypes = { "java.lang.Boolean", "java.lang.Byte", 
			"java.lang.Character", "java.lang.Short", "java.lang.Integer",
			"java.lang.Long", "java.lang.Float", "java.lang.Double", 
			"java.io.File",  "java.lang.String", "javax.management.ObjectName"
		};
		
		for (String t : simpleTypes) { 
			fcs.put(t, PRINT);
		}
		fcs.put("java.lang.Object", OMIT);
		filteredClasses = Collections.unmodifiableMap(fcs);
		
		Map<String,Integer> mcs = new HashMap<String,Integer>();
		mcs.put("getClass", OMIT);
		
		filteredMethods = Collections.unmodifiableMap(mcs);
	}
	
}
