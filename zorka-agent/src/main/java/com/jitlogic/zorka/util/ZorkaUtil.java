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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Various utility methods used by other classes. THis is singleton class as some methods need to be
 * instance methods.
 */
public class ZorkaUtil {

    /** Singleton instance */
	protected static ZorkaUtil instance;

    /** Returns singleton ZorkaUtil instance (and creates if needed) */
	public static synchronized ZorkaUtil getInstance() {
		
		if (instance == null)
			instance = new ZorkaUtil();
		
		return instance;
	}


	protected ZorkaUtil() {
	}


    /**
     * Tries to coerce a value to a specific (simple) data type.
     *
     * @param val value to be coerced (converted)
     *
     * @param c destination type class
     *
     * @return coerced value
     */
	public static Object coerce(Object val, Class<?> c) {
		
		if (val == null || c == null) { return null; }
		if (val.getClass() == c) { return val; }
		
		if (c == Long.class)    {
            return (val instanceof String) ?
                    Long.parseLong(val.toString().trim()) :
                    ((Number)val).longValue();
        }

		if (c == Integer.class) {
            return (val instanceof String) ?
                    Integer.parseInt(val.toString().trim()) :
                    ((Number)val).intValue();
        }

		if (c == Double.class)  { return (val instanceof String) ?
                Double.parseDouble(val.toString().trim()) :
                ((Number)val).doubleValue();
        }

		if (c == Short.class)   { return (val instanceof String) ?
                Short.parseShort(val.toString().trim()) :
                ((Number)val).shortValue();
        }

		if (c == Float.class)   { return (val instanceof String) ?
                Float.parseFloat(val.toString().trim()) :
                ((Number)val).floatValue();
        }

		if (c == String.class)  { return ""+val; }
		if (c == Boolean.class) { return coerceBool(val); }
		
		return null; 
	}


    /**
     * Coerces any value to boolean.
     *
     * @param val value to be coerced
     *
     * @return false if value is null or boolean false, true otherwise
     */
	public static boolean coerceBool(Object val) {
		return !(val == null || val.equals(false));
	}


    /**
     * Returns current time (in milliseconds since Epoch). This is used instead of System.currentTimeMillis(),
     * so it can be swapped to mock implementation if needed.
     *
     * @return current time (milliseconds since Epoch)
     */
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}


    /**
     * Equivalent of a.equals(b) handling edge cases (if a and/or b is null).
     *
     * @param a compared object
     *
     * @param b compared object
     *
     * @return true if both a and b are null or a equals b
     */
	public static boolean objEquals(Object a, Object b) {
		return a == null && b == null 
			|| a != null && a.equals(b);
	}


    /**
     * Compares two arrays of objects
     * @param a comapred array
     * @param b compared array
     * @return true if arrays are both null or have the same length and objects at each index are equal
     */
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


    /**
     * Returns string that contains of all elements of passed collection joined together
     *
     * @param sep separator (inserted between values)
     *
     * @param col collection of values to concatenate
     *
     * @return concatenated string
     */
    public static String join(String sep, Collection<?> col) {
        StringBuilder sb = new StringBuilder();

        for (Object val : col) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(val != null ? val.toString() : "null");
        }

        return sb.toString();
    }


    /**
     * Returns string that contains of all elements of passed (vararg) array joined together
     *
     * @param sep separator (inserted between values)
     *
     * @param vals array of values
     *
     * @return concatenated string
     */
	public static String join(String sep, Object...vals) {
		StringBuilder sb = new StringBuilder();
		
		for (Object val : vals) {
			if (sb.length() > 0) sb.append(sep);
			sb.append(val != null ? val.toString() : "null");
		}
		
		return sb.toString();
	}


    /**
     * Parses string consisting of integer and (potential) suffix (kilo, mega, giga, ...).
     *
     * @param s string to be parsed
     *
     * @return result integer value
     */
    public static long parseIntSize(String s) {

        String sn = s.trim();
        long n1 = 1;

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
                case 't':
                case 'T':
                    n1 = 1024*1024*1024*1024; break;
                default:
                    throw new NumberFormatException("Invalid size number passed: '" + s + "'");
            }
        }

        return Long.parseLong(sn) * n1;
    }

    /** Placeholder for substitution macro  */
    private final static Pattern rePropVar = Pattern.compile("\\$\\{([^\\}]+)\\}");

    /**
     * Looks for substitution macros in input string and substitutes them with values from properties collection.
     *
     * @param input input string
     *
     * @param props properties
     *
     * @return substituted string
     */
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


    /**
     * Checks if given class instance of given type. Check is done only by name, so if two classes of the same
     * name are loaded by two independent class loaders, they'll appear to be the same. This is enough if
     * object implementing given class is then accessed using reflection. As this method uses only name comparison,
     *
     * @param c class to be checked
     *
     * @param name class or interface full name (with package prefix)
     *
     * @return true if class c is a or implements type named 'name'
     */
    public static boolean instanceOf(Class<?> c, String name) {

        for (Class<?> clazz = c; !"java.lang.Object".equals(clazz.getName()); clazz = clazz.getSuperclass()) {
            if (name.equals(clazz.getName()) || interfaceOf(clazz, name)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Checks if given class implements interface of name given by ifName. Analogous to above instanceOf() method.
     *
     * @param c class to be tested
     *
     * @param ifName interface full name (with package prefix)
     *
     * @return true if class c implements interface named 'ifName'
     */
    public static boolean interfaceOf(Class<?> c, String ifName) {
        for (Class<?> ifc : c.getInterfaces()) {
            if (ifName.equals(ifc.getName()) || interfaceOf(ifc, ifName)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Clones array of bytes. Implemented by hand as JDK 1.5 does not have such method.
     *
     * @param src source array
     *
     * @return copied array
     */
    public static byte[] copyArray(byte[] src) {
        if (src == null) {
            return null;
        }

        byte[] dst = new byte[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }


    /**
     * Clones array of long integers. Implemented by hand as JDK 1.5 does not have such method.
     *
     * @param src source array
     *
     * @return copied array
     */
    public static long[] copyArray(long[] src) {
        if (src == null) {
            return null;
        }

        long[] dst = new long[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }


    /**
     * Clones array of objects of type T
     *
     * @param src source array
     *
     * @param <T> type of array items
     *
     * @return copied array
     */
    public static <T> T[] copyArray(T[] src) {
        if (src == null) {
            return null;
        }

        Class<?> arrayType = src.getClass().getComponentType();
        T[] dst = (T[])java.lang.reflect.Array.newInstance(arrayType, src.length);
        System.arraycopy(src, 0, dst, 0, src.length);

        return dst;
    }


    /**
     * Clips or extends array of objects of type T. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with null values. If passsed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     *
     * @param len target length
     *
     * @param <T> array type
     *
     * @return shortened/cloned/enlarged array
     */
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


    /**
     * Clips list if necessary. Contrasted to subList() method it creates copy of a list,
     * so all objects not copied will be garbage collected if all references to original
     * list are discarded.
     *
     * @param src original list
     *
     * @param maxSize maximum size
     *
     * @param <T> type of list elements
     *
     * @return (potentially) shortened list
     */
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


    /** Conversions used by printableASCII7 method */
    private static final String tab00c0 =
            "AAAAAAACEEEEIIII" +
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

    /**
     * Converts Unicode string to printable 7-bit ASCII characters.
     *
     * @param source unicode string
     *
     * @return 7-bit ASCII result
     */
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


    /**
     * This is useful to create a map of object in declarative way. Key-value pairs
     * are passed as arguments to this method, so call will look like this:
     * ZorkaUtil.map(k1, v1, k2, v2, ...)
     *
     * @param data keys and values (in pairs)
     *
     * @param <K> type of keys
     *
     * @param <V> type of values
     *
     * @return mutable map
     */
    public static <K, V> Map<K,V> map(Object...data) {
        Map<K,V> map = new HashMap<K, V>(data.length+2);

        for (int i = 1; i < data.length; i+=2) {
            map.put((K)data[i-1], (V)data[i]);
        }

        return map;
    }

    /**
     * Equivalent of map(k1, v1, ...) that returns constant (unmodifiable) map.
     *
     * @param data key value pairs (k1, v1, k2, v2, ...)
     *
     * @param <K> type of keys
     *
     * @param <V> type of values
     *
     * @return immutable map
     */
    public static <K,V> Map<K,V> constMap(Object...data) {
        Map<K,V> map = map(data);

        return Collections.unmodifiableMap(map);
    }
}
