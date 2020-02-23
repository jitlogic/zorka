/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * Various utility methods used by other classes. THis is singleton class as some methods need to be
 * instance methods.
 */
public class ZorkaUtil {

    private final static Logger log = LoggerFactory.getLogger(ZorkaUtil.class);

    /**
     * Singleton instance
     */
    protected final static AtomicReference<ZorkaUtil> instanceRef = new AtomicReference<ZorkaUtil>(null);

    /**
     * Returns singleton ZorkaUtil instance (and creates if needed)
     */
    public static ZorkaUtil getInstance() {

        ZorkaUtil instance = instanceRef.get();

        if (instance == null) {
            instanceRef.compareAndSet(null, new ZorkaUtil());
            instance = instanceRef.get();
        }

        return instance;
    }


    /**
     * Hidden to block direct instantiations of this class.
     */
    protected ZorkaUtil() {
    }


    /**
     * Tries to coerce a value to a specific (simple) data type.
     *
     * @param val value to be coerced (converted)
     * @param c   destination type class
     * @return coerced value
     */
    public static Object coerce(Object val, Class<?> c) {

        if (val == null || c == null) {
            return null;
        } else if (val.getClass() == c) {
            return val;
        } else if (c == String.class) {
            return castString(val);
        } else if (c == Boolean.class || c == Boolean.TYPE) {
            return coerceBool(val);
        } else if (c == Long.class || c == Long.TYPE) {
            return castLong(val);
        } else if (c == Integer.class || c == Integer.TYPE) {
            return castInteger(val);
        } else if (c == Double.class || c == Double.TYPE) {
            return castDouble(val);
        } else if (c == Short.class || c == Short.TYPE) {
            return castShort(val);
        } else if (c == Float.class || c == Float.TYPE) {
            return castFloat(val);
        }

        return null;
    }

    public static String castString(Object val) {
        try {
            return val != null ? val.toString() : "null";
        } catch (Exception e) {
            return "<ERR: " + e.getMessage() + ">";
        }
    }

    private static float castFloat(Object val) {
        return (val instanceof String)
                ? Float.parseFloat(val.toString().trim())
                : ((Number) val).floatValue();
    }

    private static short castShort(Object val) {
        return (val instanceof String)
                ? Short.parseShort(val.toString().trim())
                : ((Number) val).shortValue();
    }

    private static double castDouble(Object val) {
        return (val instanceof String)
                ? Double.parseDouble(val.toString().trim())
                : ((Number) val).doubleValue();
    }

    private static Pattern RE_INTEGER = Pattern.compile("-?\\d{1,10}");

    /** Lenient cast */
    public static Integer lcastInt(Object val) {
        if (val instanceof Number) return ((Number)val).intValue();
        if (val instanceof String && RE_INTEGER.matcher((String)val).matches()) return Integer.parseInt((String)val);
        return null;
    }

    private static int castInteger(Object val) {
        return (val instanceof String)
                ? Integer.parseInt(val.toString().trim())
                : ((Number) val).intValue();
    }

    private static long castLong(Object val) {
        return (val instanceof String)
                ? Long.parseLong(val.toString().trim())
                : ((Number) val).longValue();
    }


    /**
     * Coerces any value to boolean.
     *
     * @param val value to be coerced
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
     * @param b compared object
     * @return true if both a and b are null or a equals b
     */
    public static boolean objEquals(Object a, Object b) {
        return a == null && b == null
                || a != null && a.equals(b);
    }


    /**
     * Compares two arrays of objects
     *
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
     * @param col collection of values to concatenate
     * @return concatenated string
     */
    public static String join(String sep, Collection<?> col) {
        StringBuilder sb = new StringBuilder();

        for (Object val : col) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(castString(val));
        }

        return sb.toString();
    }


    /**
     * Returns string that contains of all elements of passed (vararg) array joined together
     *
     * @param sep  separator (inserted between values)
     * @param vals array of values
     * @return concatenated string
     */
    public static String join(String sep, Object... vals) {
        StringBuilder sb = new StringBuilder();

        for (Object val : vals) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(castString(val));
        }

        return sb.toString();
    }

    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;

    /**
     * Parses string consisting of integer and (potential) suffix (kilo, mega, giga, ...).
     *
     * @param s string to be parsed
     * @return result integer value
     */
    public static long parseIntSize(String s) {

        String sn = s.trim();
        long n1 = 1;

        if (sn.length() == 0) {
            throw new NumberFormatException("Invalid (empty) size number passed.");
        }

        char ch = sn.charAt(sn.length() - 1);

        if (Character.isLetter(ch)) {
            sn = sn.substring(0, sn.length() - 1);
            switch (ch) {
                case 'k':
                case 'K':
                    n1 = 1024;
                    break;
                case 'm':
                case 'M':
                    n1 = 1024 * 1024;
                    break;
                case 'g':
                case 'G':
                    n1 = 1024 * 1024 * 1024;
                    break;
                case 't':
                case 'T':
                    n1 = 1024 * 1024 * 1024 * 1024;
                    break;
                default:
                    throw new NumberFormatException("Invalid size number passed: '" + s + "'");
            }
        }

        return Long.parseLong(sn) * n1;
    }

    /**
     * Placeholder for substitution macro
     */
    private final static Pattern rePropVar = Pattern.compile("\\$\\{([^\\}]+)\\}");


    /**
     * Checks if given class instance of given type. Check is done only by name, so if two classes of the same
     * name are loaded by two independent class loaders, they'll appear to be the same. This is enough if
     * object implementing given class is then accessed using reflection. As this method uses only name comparison,
     *
     * @param c    class to be checked
     * @param name class or interface full name (with package prefix)
     * @return true if class c is a or implements type named 'name'
     */
    public static boolean instanceOf(Class<?> c, String name) {

        for (Class<?> clazz = c; clazz != null && !"java.lang.Object".equals(clazz.getName()); clazz = clazz.getSuperclass()) {
            if (name.equals(clazz.getName()) || interfaceOf(clazz, name)) {
                return true;
            }
        }

        return false;
    }


    public static boolean instanceOf(Class<?> c, Pattern clPattern) {

        for (Class<?> clazz = c; clazz != null && !"java.lang.Object".equals(clazz.getName()); clazz = clazz.getSuperclass()) {
            if (clPattern.matcher(clazz.getName()).matches() || interfaceOf(clazz, clPattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if given class implements interface of name given by ifName. Analogous to above instanceOf() method.
     *
     * @param c      class to be tested
     * @param ifName interface full name (with package prefix)
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


    public static boolean interfaceOf(Class<?> c, Pattern ifcPattern) {
        for (Class<?> ifc : c.getInterfaces()) {
            if (ifcPattern.matcher(ifc.getName()).matches() || interfaceOf(ifc, ifcPattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Clones array of bytes. Implemented by hand as JDK 1.5 does not have such method.
     *
     * @param src source array
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
     * @param <T> type of array items
     * @return copied array
     */
    public static <T> T[] copyArray(T[] src) {
        if (src == null) {
            return null;
        }

        Class<?> arrayType = src.getClass().getComponentType();
        T[] dst = (T[]) java.lang.reflect.Array.newInstance(arrayType, src.length);
        System.arraycopy(src, 0, dst, 0, src.length);

        return dst;
    }


    /**
     * Clips or extends array of objects of type T. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with null values. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     * @param len target length
     * @param <T> array type
     * @return shortened/cloned/enlarged array
     */
    public static <T> T[] clipArray(T[] src, int len) {

        if (src == null) {
            return null;
        }


        if (len < 0) {
            len = src.length + len > 0 ? src.length + len : 0;
        }


        Class<?> arrayType = src.getClass().getComponentType();
        T[] dst = (T[]) java.lang.reflect.Array.newInstance(arrayType, len);

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;

    }


    /**
     * Clips or extends array of objects of bytes. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with zeros. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src  source array
     * @param offs source offset
     * @param len  target length
     * @return shortened/cloned/enlarged array
     */
    public static byte[] clipArray(byte[] src, int offs, int len) {
        if (src == null) {
            return null;
        }

        if (len < 0) {
            len = src.length + len > offs ? src.length - len + offs : 0;
        }

        byte[] dst = new byte[len];

        if (len > src.length) {
            len = src.length;
        }

        if (len > 0) {
            System.arraycopy(src, offs, dst, 0, len);
        }

        return dst;
    }


    /**
     * Clips or extends array of objects of bytes. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with zeros. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     * @param len target length
     * @return shortened/cloned/enlarged array
     */
    public static byte[] clipArray(byte[] src, int len) {
        if (src == null) {
            return null;
        }

        if (len < 0) {
            len = src.length + len > 0 ? src.length + len : 0;
        }

        byte[] dst = new byte[len];

        if (len > src.length) {
            len = src.length;
        }

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;
    }

    /**
     * Clips or extends array of objects of bytes. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with zeros. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     * @param len target length
     * @return shortened/cloned/enlarged array
     */
    public static int[] clipArray(int[] src, int len) {
        if (src == null) {
            return null;
        }

        if (len < 0) {
            len = src.length + len > 0 ? src.length + len : 0;
        }

        int[] dst = new int[len];

        if (len > src.length) {
            len = src.length;
        }

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;
    }


    /**
     * Clips or extends array of objects of bytes. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with zeros. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     * @param len target length
     * @return shortened/cloned/enlarged array
     */
    public static double[] clipArray(double[] src, int len) {
        if (src == null) {
            return null;
        }

        if (len < 0) {
            len = src.length + len > 0 ? src.length + len : 0;
        }

        double[] dst = new double[len];

        if (len > src.length) {
            len = src.length;
        }

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;
    }

    /**
     * Clips or extends array of objects of bytes. If passed length is less than length of original array,
     * only so many elements of original array will be copied. If passed length is more than length of original
     * array, new elements will be filled with zeros. If passed length is the same as length of original
     * array, it is equivalent to copyArray() method.
     *
     * @param src source array
     * @param len target length
     * @return shortened/cloned/enlarged array
     */
    public static long[] clipArray(long[] src, int len) {
        if (src == null) {
            return null;
        }

        if (len < 0) {
            len = src.length + len > 0 ? src.length + len : 0;
        }

        long[] dst = new long[len];

        if (len > src.length) {
            len = src.length;
        }

        if (len > 0) {
            System.arraycopy(src, 0, dst, 0, len);
        }

        return dst;
    }


    public static int[] intArray(List<Integer> l) {
        int[] a = new int[l.size()];

        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }

        return a;
    }


    public static long[] longArray(List<Long> l) {
        long[] a = new long[l.size()];

        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }

        return a;
    }


    public static double[] doubleArray(List<Double> l) {
        double[] a = new double[l.size()];

        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }

        return a;
    }


    /**
     * Clips list if necessary. Contrasted to subList() method it creates copy of a list,
     * so all objects not copied will be garbage collected if all references to original
     * list are discarded.
     *
     * @param src     original list
     * @param maxSize maximum size
     * @param <T>     type of list elements
     * @return (potentially) shortened list
     */
    public static <T> List<T> clip(List<T> src, int maxSize) {

        if (src.size() <= maxSize) {
            return src;
        }

        List<T> lst = new ArrayList<T>(maxSize + 2);

        for (int i = 0; i < maxSize; i++) {
            lst.add(src.get(i));
        }

        return lst;
    }


    /**
     * Conversions used by printableASCII7 method
     */
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

            if (one < (char) 32 || one > (char) 126) {
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
     * @param <K>  type of keys
     * @param <V>  type of values
     * @return mutable map
     */
    public static <K, V> HashMap<K, V> map(Object... data) {
        HashMap<K, V> map = new HashMap<K, V>(data.length + 2);

        for (int i = 1; i < data.length; i += 2) {
            map.put((K) data[i - 1], (V) data[i]);
        }

        return map;
    }


    /**
     * This is useful to create a map of object in declarative way. Key-value pairs
     * are passed as arguments to this method, so call will look like this:
     * ZorkaUtil.map(k1, v1, k2, v2, ...)
     *
     * @param data keys and values (in pairs)
     * @param <K>  type of keys
     * @param <V>  type of values
     * @return mutable map
     */
    public static <K, V> Map<K, V> lmap(Object... data) {
        Map<K, V> map = new LinkedHashMap<K, V>(data.length + 2);

        for (int i = 1; i < data.length; i += 2) {
            map.put((K) data[i - 1], (V) data[i]);
        }

        return map;
    }


    /**
     * Equivalent of map(k1, v1, ...) that returns constant (unmodifiable) map.
     *
     * @param data key value pairs (k1, v1, k2, v2, ...)
     * @param <K>  type of keys
     * @param <V>  type of values
     * @return immutable map
     */
    public static <K, V> Map<K, V> constMap(Object... data) {
        Map<K, V> map = map(data);

        return Collections.unmodifiableMap(map);
    }


    /**
     * Creates a set from supplied strings.
     *
     * @param objs members of newly formed set
     * @return set of strings
     */
    public static <T> Set<T> set(T...objs) {
        Set<T> set = new HashSet<T>(objs.length * 2 + 1);
        Collections.addAll(set, objs);

        return set;
    }

    public static <T> Set<T> constSet(T...objs) {
        Set<T> set = set(objs);

        return Collections.unmodifiableSet(set);
    }


    public static String path(String... components) {
        StringBuilder sb = new StringBuilder();

        for (String s : components) {

            if (s.endsWith("/") || s.endsWith("\\")) {
                s = s.substring(0, s.length() - 1);
            }

            if (sb.length() == 0) {
                sb.append(s);
            } else {
                if (s.startsWith("/") || s.startsWith("\\")) {
                    s = s.substring(1);
                }
                sb.append("/");
                sb.append(s);
            }
        }

        return sb.toString().replace('\\', '/');
    }


    public static String strClock(long clock) {
        return new Date(clock).toString();
    }

    public static String strTime(long ns) {
        double t = 1.0 * ns / 1000000.0;
        String u = "ms";
        if (t > 1000.0) {
            t /= 1000.0;
            u = "s";
        }
        return String.format(t > 10 ? "%.0f" : "%.2f", t) + u;
    }


    public static int iparam(Map<String, String> params, String name, int defval) {
        try {
            return params.containsKey(name) ? Integer.parseInt(params.get(name)) : defval;
        } catch (NumberFormatException e) {
            return defval;
        }
    }

    public static void rmrf(String path) throws IOException {
        rmrf(new File(path));
    }


    public static void rmrf(File f) throws IOException {
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    rmrf(c);
                }
            }
            f.delete();
        }
    }


    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static int h2i(char c) {
        if (c >= '0' && c <= '9') {
            return c-'0';
        } else if (c >= 'a' && c <= 'f') {
            return c-'a'+10;
        } else if (c >= 'A' && c <= 'F') {
            return c-'A'+10;
        } else {
            throw new ZorkaRuntimeException("This is not hex character: '" + c + "'");
        }
    }

    public static byte[] hex(String s) {
        if (0 != (s.length()&0x01)) throw new ZorkaRuntimeException("Malformed hex string: " + s);
        byte[] rslt = new byte[s.length()>>1];
        for (int i = 0; i < s.length(); i+=2) {
            rslt[i>>1] = (byte)((h2i(s.charAt(i))<<4)|h2i(s.charAt(i+1)));
        }
        return rslt;
    }

    public static String hex(byte[] input) {
        return hex(input, input.length);
    }

    public static String hex(byte[] input, int len) {
        StringBuffer sb = new StringBuffer(input.length * 2);

        for (int i = 0; i < len; i++) {
            int c = input[i] & 0xff;
            sb.append(HEX[(c >> 4) & 0x0f]);
            sb.append(HEX[c & 0x0f]);
        }

        return sb.toString();
    }

    public static String hex(long l) {
        return String.format("%08x%08x", l >>> 32, l & 0xffffffffL);
    }

    public static String hex(long l1, long l2) {
        return String.format("%08x%08x%08x%08x", l1 >>> 32, l1 & 0xffffffffL, l2 >>> 32, l2 & 0xffffffffL);
    }

    public static final Pattern RE_HEX64 = Pattern.compile("([0-9a-fA-F]{8})([0-9a-fA-F]{8})");

    public static long unhex64(String hstr) {
        if (hstr == null) throw new NullPointerException("Null HEX64 string");
        Matcher m = RE_HEX64.matcher(hstr);
        if (m.matches()) {
            return Long.parseLong(m.group(1),16) << 32 | Long.parseLong(m.group(2),16);
        } else {
            throw new ZorkaRuntimeException("Invalid HEX64 string: '" + hstr + "'");
        }
    }

    public static final Pattern RE_HEX128 = Pattern.compile("([0-9a-fA-F]{8})([0-9a-fA-F]{8})([0-9a-fA-F]{8})([0-9a-fA-F]{8})");

    public static long[] unhex128(String hstr) {
        if (hstr == null) throw new NullPointerException("Null HEX128 string");
        Matcher m = RE_HEX128.matcher(hstr);
        if (m.matches()) {
            return new long[] {
                Long.parseLong(m.group(1),16) << 32 | Long.parseLong(m.group(2),16),
                Long.parseLong(m.group(3),16) << 32 | Long.parseLong(m.group(4),16)
            };
        } else {
            throw new ZorkaRuntimeException("Invalid HEX128 string");
        }
    }


    public static String crc32(String input) {
        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes());
        return String.format("%08x", crc32.getValue());
    }


    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            return hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Not supported digest algorithm: 'MD5'");
        }
        return null;
    }


    public static String sha1(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA1");
            sha.update(input.getBytes());
            return hex(sha.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Not supported digest algorithm: 'SHA1'");
        }
        return null;
    }

    public static String generateKey() {
        SecureRandom rand = new SecureRandom();
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        rand.nextBytes(key);
        rand.nextBytes(iv);

        return ZorkaUtil.hex(key) + "." + ZorkaUtil.hex(iv);
    }


    public static byte[][] parseKey(String s) {
        String[] parts = s.split("\\.");
        if (parts.length != 2 || parts[0].length() != 32 || parts[1].length() != 32) {
            throw new ZorkaRuntimeException("Malformed encyrption key: " + s);
        }
        return new byte[][]{hex(parts[0]),hex(parts[1])};
    }


    public static byte[] aes(int mode, byte[] data, byte[][] kv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec sks = new SecretKeySpec(kv[0], "AES");
            IvParameterSpec ips = new IvParameterSpec(kv[1]);
            cipher.init(mode, sks, ips);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        } catch (NoSuchPaddingException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        } catch (InvalidKeyException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        } catch (BadPaddingException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        } catch (IllegalBlockSizeException e) {
            throw new ZorkaRuntimeException("in ZorkaUtil.encrypt()", e);
        }
    }

    public static void sleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
        }
    }

    public static byte[] slurp(InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int len = 0;
        while ((len = is.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    public static String urlEncode(String s) {

        if (s == null || s.length() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? ""+c : "%"+Integer.toHexString((int)c));
        }
        return sb.toString();
    }

    public static void reverseList(List lst) {
        for (int i = 0; i < lst.size()/2; i++) {
            Object obj = lst.get(i);
            lst.set(i, lst.get(lst.size()-i-1));
            lst.set(lst.size()-i-1, obj);
        }
    }

    public static void close(Closeable obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                log.warn("Cannot close", e);
            }
        }
    }

    public static void close(Socket obj) {
        if (obj != null) {
            try {
                obj.close();
            } catch (IOException e) {
                log.warn("Cannot close", e);
            }
        }
    }

    public static byte[] gzip(byte[] buf) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(buf);
            gos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new ZorkaRuntimeException("Error gzipping data", e);
        }
    }

    public static byte[] gunzip(byte[] buf) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(buf));
            return slurp(gis);
        } catch (Exception e) {
            throw new ZorkaRuntimeException("Error unzipping data", e);
        }
    }

}
