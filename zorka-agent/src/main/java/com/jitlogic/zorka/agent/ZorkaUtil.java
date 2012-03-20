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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.CompositeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZorkaUtil {
	
	private final static Logger log = LoggerFactory.getLogger(ZorkaUtil.class);
	
	public static void loadProps(String urlpath, Properties props) {
		InputStream is = null;
		try {
			if (urlpath.startsWith("classpath://"))
				is = ZorkaUtil.class.getResourceAsStream(urlpath.substring(12));
			else if (urlpath.startsWith("file://"))
				is = new FileInputStream(urlpath.substring(7));
			else {
				URL url = new URL(urlpath);
				is = url.openStream();
			}
			props.load(is);
		} catch (IOException e) {
			log.error("I/O error while reading properties file '" 
					+ urlpath + "': " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (is != null)
				try { is.close(); } catch (IOException e) { }
		}
	}
	
	public static String readText(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String nl = System.getProperty("line.separator");
		String line;
		
		try {
			while (null != (line = rdr.readLine())) {
				sb.append(line);
				sb.append(nl);
			}
		} finally {
			rdr.close();
		}
		
		return sb.toString();
	}
	
	public static String errorDump(Throwable e) {
		Writer rslt = new StringWriter();
		PrintWriter pw = new PrintWriter(rslt);
		e.printStackTrace(pw);
		return e.getMessage() + "\n" + rslt;
	}
	
	
	public static String objectDump(Object o) {
		// TODO zrobić poprawną introspekcję tutaj 
		return ""+o;
	}
	
	public static Object coerce(Object obj, Class<?> c) {
		
		if (obj == null || c == null) return null;
		if (obj.getClass() == c) return obj;
		
		if (c == Long.class)    return ((Number)obj).longValue();
		if (c == Integer.class) return ((Number)obj).intValue();
		if (c == Double.class)  return ((Number)obj).doubleValue();
		if (c == Short.class)   return ((Number)obj).shortValue();
		if (c == Float.class)   return ((Number)obj).floatValue();
		if (c == String.class)  return ""+obj;
		if (c == Boolean.class) return coerceBool(obj);
		
		return null; 
	}
	
	
	public static boolean coerceBool(Object obj) {
		return !(obj == null || obj.equals(false));
	}

	public static Method lookupMethod(Class<?> clazz, String name) {
		try {
			return clazz.getMethod(name);
		} catch (NoSuchMethodException e) {
			for (Class<?> icl : clazz.getInterfaces()) {
				Method m = lookupMethod(icl, name);
				if (m != null) return m;
			}
			Class<?> mcl = clazz;
			while ((mcl = clazz.getSuperclass()) != null) {
				Method m = lookupMethod(mcl, name);
				if (m != null) return m;
			}
		}
		return null;
	}
	
	public static Method lookupGetter(Class<?> clazz, String name) {
		Method m = lookupMethod(clazz, "get" + name.substring(0,1).toUpperCase() + name.substring(1));
		if (m != null) return m;
		m = lookupMethod(clazz, "is" + name.substring(0, 1) + name.substring(1));
		if (m != null) return m;
		m = lookupMethod(clazz, name);
		return m;
	}

	
	// TODO przenieść do dedykowanego obiektu i zrefaktorować
	public static Object get(Object obj, Object key) {
		if (obj == null) {
			return null;
		} else if (obj instanceof Map<?, ?>) {
			return ((Map<?,?>)obj).get(key);
		} else if (obj instanceof List<?>) {
			Integer idx = (Integer)coerce(key, Integer.class);
			return idx != null ? ((List<?>)obj).get(idx) : null;
		} else if (obj.getClass().isArray()) {
			Integer idx = (Integer)coerce(key, Integer.class);
			return idx != null ? ((Object[])obj)[idx] : null;
		}		
		if (obj instanceof CompositeData) {
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
			Method m = lookupGetter(clazz, name);
			if (m != null)
				try {
					return m.invoke(obj);
				} catch (Exception e) {
					// TODO zalogowac problem 
					return null;
				}
			
			// Try field (if any)
			try {
				Field field = clazz.getField(name);
				return field.get(name);
			} catch (Exception e) {
				// TODO zalogowac problem
				return null;
			}
		}
			
		return null;
	}
	
	// TODO przenieść do dedykowanego obiektu i zrefaktorować
	public static List<String> listAttrNames(Object obj) {
		List<String> lst = new ArrayList<String>();
		if (obj instanceof Map) {
			for (Object key : ((Map<?,?>)obj).keySet())
				lst.add(key.toString());
		} else if (obj instanceof Stats) {
			for (String name : ((Stats)obj).getStatisticNames()) {
				lst.add(name);
			}
		}
		// TODO uzupelnic 
		return lst;
	}
	
	/**
	 * 
	 * @param v
	 * @return
	 */
	public static String valueDump(Object v) {
		return ""+v;
	}
	
	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	
	public static boolean objEquals(Object a, Object b) {
		return a == null && b == null 
			|| a != null && a.equals(b);
	}
	
	public static boolean fullStackDumps = true;
	
	public static void error(Logger log, String msg, Throwable e) {
		if (fullStackDumps)
			log.error(msg, e);
		else
			log.error(msg + ": " + e.getMessage());
	}
}
