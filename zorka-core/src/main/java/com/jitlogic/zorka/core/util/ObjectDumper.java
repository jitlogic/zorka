/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.util;

import com.jitlogic.zorka.common.stats.ZorkaStat;
import com.jitlogic.zorka.common.stats.ZorkaStats;
import com.jitlogic.zorka.common.util.JmxObject;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

/**
 * Utility class that implement object dump functionality.
 * Object dumps are human readable representations of objects
 * and their attributes. Object dumper can recursively display attributes
 * of objects qualify for further traversal.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class ObjectDumper {

    /** Classes that won't be recursively traversed */
    private static final Map<String,Integer> filteredClasses;

    /** Getter methods that won't be recursively traversed */
	private static final Map<String,Integer> filteredMethods;

    /** Marks classes (methods) that should be printed using toString() instead of full traversal */
	private static final int PRINT = 1;

    /** Marks classes (methods) that should be omitted completely */
	private static final int OMIT = 2;

    /** "Tab" string */
	private static final String LEAD = "    ";

    /** Maximum depth */
	private static final int MAX_DEPTH = 8;

    /** Screen width (estimated) */
	private static final int SCREEN_WIDTH = 120;
	
	/** Hide constructor for utility class */
	private ObjectDumper() {
    }

    /** Prints exception object */
	public static String errorDump(Throwable e) {
		Writer rslt = new StringWriter();
		PrintWriter pw = new PrintWriter(rslt);
		e.printStackTrace(pw);
		return e.getMessage() + "\n" + rslt;
	}
	
    /** Traverses and prints arbitrary object */
	public static String objectDump(Object obj) {
		StringBuilder sb = new StringBuilder();
		dump("", obj, sb, 0);
		return sb.toString();
	}

    /**
     * Dumps arbitrary object (delegates actual serialization to other methods)
     *
     * @param lead lead spaces
     *
     * @param obj object
     *
     * @param sb strin builder collecting output
     *
     * @param depth current recursion depth
     */
	private static void dump(String lead, Object obj, StringBuilder sb, int depth) {
		
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
		
			if (c == OMIT) {
                return;
            }
		}

		if (depth > MAX_DEPTH) {
			sb.append("<...>");
			return;
		}
		
		if (obj instanceof JmxObject) {
			dumpJmxObject(lead, obj, sb, depth);
		} else if (obj instanceof Collection) {
			dumpCollection(lead, obj, sb, depth);
		} else if (obj instanceof Map) {
			dumpMap(lead, obj, sb, depth);
        } else if (ZorkaUtil.instanceOf(obj.getClass(), "javax.management.j2ee.statistics.Stats")) {
            dumpStats(lead, obj, sb, depth);
        } else if (obj instanceof ZorkaStats) {
            dumpZorkaStats(lead, obj, sb, depth);
		} else if (obj instanceof CompositeData) {
			dumpCompositeData(lead, obj, sb, depth);
		} else if (obj instanceof TabularData) {
			dumpTabularData(lead, obj, sb, depth);
		} else {
			dumpPojo(lead, obj, sb, depth);
		}
	}


    /**
     * Dumps ordinary POJO object.
     *
     * @param lead lead spaces
     *
     * @param obj object
     *
     * @param sb string builder collecting output data
     *
     * @param depth current recursion depth
     */
	private static void dumpPojo(String lead, Object obj, StringBuilder sb, int depth) {
		sb.append("\n");
		for (Method m : obj.getClass().getMethods()) {
			String name = m.getName();
			if (m.getParameterTypes().length != 0) { continue; }
			if (!name.startsWith("get") && !name.startsWith("is")) { continue; }
			Integer cm = filteredMethods.get(name);
			if (cm != null && cm.equals(OMIT)) { continue; }
			name = name.startsWith("get") ? name.substring(3) : name.substring(2);
			name = name.substring(0,1).toLowerCase() + name.substring(1);
			sb.append(lead);
            sb.append(name);
            sb.append(" : ");
			sb.append(m.getReturnType().getName());
            sb.append(" = ");
			try {
				Object o = m.invoke(obj);
				dump(lead + LEAD, o, sb, depth + 1);
			} catch (Exception e) {
				sb.append("<error: " + e.getMessage() + ">");
			}
		}
	}


    /**
     * Dumps JMX tabular data object
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
	private static void dumpTabularData(String lead, Object obj, StringBuilder sb, int depth) {
		TabularData td = (TabularData)obj;
		for (Object ksObj : td.keySet()) {
			sb.append(lead);
			dump(lead + LEAD, ksObj, sb, depth + 1);
			sb.append("\n");
		}
	}


    /**
     * Dumps JMX composite data object
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
	private static void dumpCompositeData(String lead, Object obj, StringBuilder sb, int depth) {
		CompositeData data = (CompositeData)obj;
		CompositeType type = data.getCompositeType();
		sb.append("\n");
		for (Object o : type.keySet()) {
			String name = o.toString();
			Object value = data.get(name);
			sb.append(lead);
            sb.append(name);
            sb.append(" : ");
			if (value != null) {
				sb.append(value.getClass().getName());
				sb.append(" = ");
				dump(lead + LEAD, value, sb, depth + 1);
			} else {
				sb.append("(?) = <null>");
			}
			sb.append("\n");
		}
	}


    /**
     * Dumps J2EE statistics object.
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
    private static void dumpStats(String lead, Object obj, StringBuilder sb, int depth)  {
        try {
            Method m = obj.getClass().getMethod("getStatistics");
            for (Object o : (Object[])m.invoke(obj)) {
                if (o == null) { continue; }
                Method m2 = o.getClass().getMethod("getName");
                String name = (String)m2.invoke(obj);
                sb.append(lead);
                sb.append(name);
                sb.append(" : ");
                sb.append(o.getClass().getName());
                sb.append(" = ");
                dump(lead + LEAD, o, sb, depth + 1);
            }
        } catch (Exception e) {
            sb.append("<error " + e.getMessage() + ">");
        }
    }


    /**
     * Dumps ZorkaStats object
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
	private static void dumpZorkaStats(String lead, Object obj, StringBuilder sb, int depth) {
		ZorkaStats stats = (ZorkaStats)obj;
		for (String sn : stats.getStatisticNames()) {
            ZorkaStat s = stats.getStatistic(sn);
			sb.append(lead);
            sb.append(s.getName());
            sb.append(" : ");
			sb.append(s.getClass().getName());
            sb.append(" = ");
			dump(lead + LEAD, s, sb, depth + 1);
		}
	}


    /**
     * Dumps Java map object
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
    private static void dumpMap(String lead, Object obj, StringBuilder sb, int depth) {
		Map<?,?> map = (Map<?,?>)obj;
		sb.append("{");
		int pos = sb.length();
		for (Entry<?, ?> e : map.entrySet()) {
			sb.append(e.getKey());
			sb.append(" : ");
			dump(lead + LEAD, e.getValue(), sb, depth + 1);
			pos = checkNewLine(lead, sb, pos);				
		}
		sb.append("}");
	}


    /**
     * Dumps collection object
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
    private static void dumpCollection(String lead, Object obj, StringBuilder sb, int depth) {
		Collection<?> col = (Collection<?>)obj;
		sb.append("[");
		int pos = sb.length();
		for (Object o : col) {
			dump(lead + LEAD, o, sb, depth + 1);
			sb.append(",");
			pos = checkNewLine(lead, sb, pos);
		}
		sb.append("]");
	}


    /**
     * Dumps JMX mbean object.
     *
     * @param lead lead spaces
     *
     * @param obj object to be dumped
     *
     * @param sb output string buffer
     *
     * @param depth current recursion depth
     */
	private static void dumpJmxObject(String lead, Object obj, StringBuilder sb, int depth) {
		JmxObject jmx = (JmxObject)obj;
		sb.append(jmx.getName()); sb.append(":\n");
		try {
			MBeanInfo mi = jmx.getConn().getMBeanInfo(jmx.getName());
			for (MBeanAttributeInfo mbi : mi.getAttributes()) {
				try {
					Object o = jmx.getConn().getAttribute(jmx.getName(), mbi.getName());
					sb.append(lead);
                    sb.append(mbi.getName());
                    sb.append(" : ");
					sb.append(mbi.getType());
                    sb.append(" = ");
					if (o != obj) {
						dump(lead + LEAD, o, sb, depth + 1);
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
	

    /** Checks if new line is needed. */
	private static int checkNewLine(String lead, StringBuilder sb, int pos) {
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
