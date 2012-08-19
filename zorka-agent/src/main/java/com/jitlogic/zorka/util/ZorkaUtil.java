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

package com.jitlogic.zorka.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.j2ee.statistics.Stats;


public class ZorkaUtil {
	
	public final ZorkaLog log = ZorkaLogger.getLog(this.getClass());
	
	protected static ZorkaUtil instance;
	
	public static synchronized ZorkaUtil getInstance() {
		
		if (instance == null)
			instance = new ZorkaUtil();
		
		return instance;
	}
	
	protected ZorkaUtil() {
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

	
	public static Object coerce(Object obj, Class<?> c) {
		
		if (obj == null || c == null) { return null; }
		if (obj.getClass() == c) { return obj; }
		
		if (c == Long.class)    { return ((Number)obj).longValue(); }
		if (c == Integer.class) { return ((Number)obj).intValue(); }
		if (c == Double.class)  { return ((Number)obj).doubleValue(); }
		if (c == Short.class)   { return ((Number)obj).shortValue(); }
		if (c == Float.class)   { return ((Number)obj).floatValue(); }
		if (c == String.class)  { return ""+obj; }
		if (c == Boolean.class) { return coerceBool(obj); }
		
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
				if (m != null) {
					return m;
				}
			}
			Class<?> mcl = clazz;
			while ((mcl = clazz.getSuperclass()) != null) {
				Method m = lookupMethod(mcl, name);
				if (m != null) {
					return m;
				}
			}
		}
		return null;
	}
	
	public static Method lookupGetter(Class<?> clazz, String name) {
		Method m = lookupMethod(clazz, "get" + name.substring(0,1).toUpperCase() + name.substring(1));
		if (m != null) { 
			return m; 
		}
		m = lookupMethod(clazz, "is" + name.substring(0, 1) + name.substring(1));
		if (m != null) {
			return m;
		}
		m = lookupMethod(clazz, name);
		return m;
	}

	
	// TODO przenieść do dedykowanego obiektu i zrefaktorować
	public static List<String> listAttrNames(Object obj) {
		List<String> lst = new ArrayList<String>();
		if (obj instanceof Map) {
			for (Object key : ((Map<?,?>)obj).keySet()) {
				lst.add(key.toString());
			}
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
	
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}
	
	
	public static boolean objEquals(Object a, Object b) {
		return a == null && b == null 
			|| a != null && a.equals(b);
	}
	
	public static boolean arrayEquals(Object[] a, Object[] b) {
		
		if (a == null || b == null) {
			return a == null && b == null;
		}
		
		if (a.length != b.length) {
			return false;
		}
		
		for (int i = 0; i < a.length; i++)
			if (!objEquals(a[i], b[i]))
				return false;
		
		return true;
	}
	
	public static String join(String sep, Object...vals) {
		StringBuilder sb = new StringBuilder();
		
		for (Object val : vals) {
			if (sb.length() > 0) sb.append(sep);
			sb.append(val != null ? val.toString() : "null");
		}
		
		return sb.toString();
	}
}
