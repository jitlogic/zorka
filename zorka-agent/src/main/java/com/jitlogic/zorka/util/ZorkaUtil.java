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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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


	public static Object coerce(Object obj, Class<?> c) {
		
		if (obj == null || c == null) { return null; }
		if (obj.getClass() == c) { return obj; }
		
		if (c == Long.class)    {
            return (obj instanceof String) ?
                    Long.parseLong(obj.toString().trim()) :
                    ((Number)obj).longValue();
        }

		if (c == Integer.class) {
            return (obj instanceof String) ?
                    Integer.parseInt(obj.toString().trim()) :
                    ((Number)obj).intValue();
        }

		if (c == Double.class)  { return (obj instanceof String) ?
                Double.parseDouble(obj.toString().trim()) :
                ((Number)obj).doubleValue();
        }

		if (c == Short.class)   { return (obj instanceof String) ?
                Short.parseShort(obj.toString().trim()) :
                ((Number)obj).shortValue();
        }

		if (c == Float.class)   { return (obj instanceof String) ?
                Float.parseFloat(obj.toString().trim()) :
                ((Number)obj).floatValue();
        }

		if (c == String.class)  { return ""+obj; }
		if (c == Boolean.class) { return coerceBool(obj); }
		
		return null; 
	}
	
	
	public static boolean coerceBool(Object obj) {
		return !(obj == null || obj.equals(false));
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


    public static String join(String sep, Collection<?> col) {
        StringBuilder sb = new StringBuilder();

        for (Object val : col) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(val != null ? val.toString() : "null");
        }

        return sb.toString();
    }


	public static String join(String sep, Object...vals) {
		StringBuilder sb = new StringBuilder();
		
		for (Object val : vals) {
			if (sb.length() > 0) sb.append(sep);
			sb.append(val != null ? val.toString() : "null");
		}
		
		return sb.toString();
	}


    public static int parseIntSize(String s) {

        String sn = s.trim();
        int n1 = 1;

        if (sn.length() == 0) {
            throw new NumberFormatException("Invalid (empty) size number passed.");
        }

        char ch = sn.charAt(sn.length()-1);

        if (Character.isLetter(ch)) {
            sn = sn.substring(0, sn.length()-1);
            switch (ch) {
                case 'k':
                case 'K':
                    n1 = 1024; break;
                case 'm':
                case 'M':
                    n1 = 1024*1024; break;
                case 'g':
                case 'G':
                    n1 = 1024*1024*1024; break;
                default:
                    throw new NumberFormatException("Invalid size number passed: '" + s + "'");
            }
        }

        return Integer.parseInt(sn) * n1;
    }

    private final static Pattern rePropVar = Pattern.compile("\\$\\{([^\\}]+)\\}");

    public static String evalPropStr(String input, Properties props){
        StringBuffer sb = new StringBuffer(input.length()+50);

        Matcher matcher = rePropVar.matcher(input);

        while (matcher.find()) {
            String var = matcher.group(1);
            if (props.containsKey(var)) {
                matcher.appendReplacement(sb, props.get(var).toString());
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }


    public static boolean instanceOf(Class<?> c, String name) {

        for (Class<?> clazz = c; !"java.lang.Object".equals(clazz.getName()); clazz = clazz.getSuperclass()) {
            if (name.equals(clazz.getName()) || interfaceOf(clazz, name)) {
                return true;
            }
        }

        return false;
    }


    public static boolean interfaceOf(Class<?> c, String ifName) {
        for (Class<?> ifc : c.getInterfaces()) {
            if (ifName.equals(ifc.getName()) || interfaceOf(ifc, ifName)) {
                return true;
            }
        }

        return false;
    }

    public static byte[] copyArray(byte[] src) {
        if (src == null) {
            return null;
        }

        byte[] dst = new byte[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    public static long[] copyArray(long[] src) {
        if (src == null) {
            return null;
        }

        long[] dst = new long[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }


    public static <T> T[] copyArray(T[] src) {
        if (src == null) {
            return null;
        }

        Class<?> arrayType = src.getClass().getComponentType();
        T[] dst = (T[])java.lang.reflect.Array.newInstance(arrayType, src.length);
        System.arraycopy(src, 0, dst, 0, src.length);

        return dst;
    }

    public static <T> T[] clipArray(T[] src, int len) {

        if (src == null) {
            return null;
        }


        if (len < 0) {
            len = src.length - len > 0 ? src.length - len : 0;
        }


        Class<?> arrayType = src.getClass().getComponentType();
        T[] dst = (T[])java.lang.reflect.Array.newInstance(arrayType, len);

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;

    }


    public static <T> List<T> clip(List<T> src, int maxSize) {

        if (src.size() <= maxSize) {
            return src;
        }

        List<T> lst = new ArrayList<T>(maxSize+2);

        for (int i = 0; i < maxSize; i++) {
            lst.add(src.get(i));
        }

        return lst;
    }


    private static final String tab00c0 = "AAAAAAACEEEEIIII" +
            "DNOOOOO\u00d7\u00d8UUUUYI\u00df" +
            "aaaaaaaceeeeiiii" +
            "\u00f0nooooo\u00f7\u00f8uuuuy\u00fey" +
            "AaAaAaCcCcCcCcDd" +
            "DdEeEeEeEeEeGgGg" +
            "GgGgHhHhIiIiIiIi" +
            "IiJjJjKkkLlLlLlL" +
            "lLlNnNnNnnNnOoOo" +
            "OoOoRrRrRrSsSsSs" +
            "SsTtTtTtUuUuUuUu" +
            "UuUuWwYyYZzZzZzF";

    public static String printableASCII7(String source) {
        char[] vysl = new char[source.length()];
        char one;

        for (int i = 0; i < source.length(); i++) {
            one = source.charAt(i);
            if (one >= '\u00c0' && one <= '\u017f') {
                one = tab00c0.charAt((int) one - '\u00c0');
            }

            if (one < (char)32 || one > (char)126) {
                one = '.';
            }

            vysl[i] = one;
        }

        return new String(vysl);
    }


    public static <K, V> Map<K,V> map(Object...data) {
        Map<K,V> map = new HashMap<K, V>(data.length+2);

        for (int i = 1; i < data.length; i+=2) {
            map.put((K)data[i-1], (V)data[i]);
        }

        return map;
    }

    public static <K,V> Map<K,V> constMap(Object...data) {
        Map<K,V> map = map(data);

        return Collections.unmodifiableMap(map);
    }
}
