/**
 * Shamlessly copied from StringTree project:
 *
 * http://sourceforge.net/projects/stringtree.
 *
 * StringTree package is available under Apache License or GPLv2 license.
 *
 */

package com.jitlogic.zorka.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// TODO bean serialization this only covers basic cases, so it is not complete nor secure;
// TODO implement useful set of unit tests, refactor and implement corner cases;

public class JSONReader {

    protected static final Object OBJECT_END = new Object();
    protected static final Object ARRAY_END = new Object();
    protected static final Object COLON = new Object();
    protected static final Object COMMA = new Object();
    public static final int FIRST = 0;
    public static final int CURRENT = 1;
    public static final int NEXT = 2;

    protected static Map<Character, Character> escapes = new HashMap<Character, Character>();
    static {
        escapes.put(Character.valueOf('"'), Character.valueOf('"'));
        escapes.put(Character.valueOf('\\'), Character.valueOf('\\'));
        escapes.put(Character.valueOf('/'), Character.valueOf('/'));
        escapes.put(Character.valueOf('b'), Character.valueOf('\b'));
        escapes.put(Character.valueOf('f'), Character.valueOf('\f'));
        escapes.put(Character.valueOf('n'), Character.valueOf('\n'));
        escapes.put(Character.valueOf('r'), Character.valueOf('\r'));
        escapes.put(Character.valueOf('t'), Character.valueOf('\t'));
    }

    protected CharacterIterator it;
    protected char c;
    protected Object token;
    protected StringBuffer buf = new StringBuffer();
    
    public void reset() {
    	it = null;
    	c = 0;
    	token = null;
    	buf.setLength(0);
    }

    protected char next() {
        c = it.next();
        return c;
    }

    protected void skipWhiteSpace() {
        while (Character.isWhitespace(c)) {
            next();
        }
    }

    public Object read(CharacterIterator ci, int start, Class<?> clazz) {
    	reset();
        it = ci;
        switch (start) {
        case FIRST:
            c = it.first();
            break;
        case CURRENT:
            c = it.current();
            break;
        case NEXT:
            c = it.next();
            break;
        }
        return read(clazz);
    }

    public Object read(CharacterIterator it, Class<?> clazz) {
        return read(it, NEXT, clazz);
    }

    public Object read(String string) {
        return read(string, Object.class);
    }

    public <T> T read(String string, Class<T> clazz) {
        return (T)read(new StringCharacterIterator(string), FIRST, clazz);
    }

    protected Object read(Class<?> clazz) {
        skipWhiteSpace();
        char ch = c;
        next();
        switch (ch) {
            case '"': token = string(); break;
            case '[': token = array(clazz); break;
            case ']': token = ARRAY_END; break;
            case ',': token = COMMA; break;
            case '{':
                if (clazz != Object.class) {
                    token = bean(clazz);
                } else {
                    token = object(clazz);
                }
                break;
            case '}': token = OBJECT_END; break;
            case ':': token = COLON; break;
            case 't':
                next(); next(); next(); // assumed r-u-e
                token = Boolean.TRUE;
                break;
            case'f':
                next(); next(); next(); next(); // assumed a-l-s-e
                token = Boolean.FALSE;
                break;
            case 'n':
                next(); next(); next(); // assumed u-l-l
                token = null;
                break;
            default:
                c = it.previous();
                if (Character.isDigit(c) || c == '-') {
                    token = number();
                }
        }
        // System.out.println("token: " + token); // enable this line to see the token stream
        return token;
    }

    protected <T> T bean(Class<T> clazz) {
        T obj = ObjectInspector.instantiate(clazz);

        String key = (String)read(String.class);


        while (token != OBJECT_END) {
            Field field = ObjectInspector.lookupField(clazz, key);
            Class<?> fieldClazz = field != null ? field.getType() : Object.class;
            if (fieldClazz == List.class || ZorkaUtil.interfaceOf(fieldClazz, "java.util.List")) {
                fieldClazz = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
            }
            read(Object.class); // should be a colon
            if (token != OBJECT_END) {
                ObjectInspector.setField(obj, key, read(fieldClazz));
                if (read(Object.class) == COMMA) {
                    key = (String)read(String.class);
                }
            }
        }

        return obj;
    }

    protected Object object(Class<?> clazz) {
        Map<Object, Object> ret = new LinkedHashMap<Object, Object>();
        Object key = read(clazz);
        while (token != OBJECT_END) {
            read(clazz); // should be a colon
            if (token != OBJECT_END) {
                ret.put(key, read(clazz));
                if (read(clazz) == COMMA) {
                    key = read(clazz);
                }
            }
        }

        return ret;
    }

    protected Object array(Class<?> clazz) {
        List ret = new ArrayList();
        Object value = read(clazz);
        while (token != ARRAY_END) {
            ret.add(value);
            if (read(clazz) == COMMA) {
                value = read(clazz);
            }
        }
        return ret;
    }

    protected Object number() {
        int length = 0;
        boolean isFloatingPoint = false;
        buf.setLength(0);

        if (c == '-') {
            add();
        }
        length += addDigits();
        if (c == '.') {
            add();
            length += addDigits();
            isFloatingPoint = true;
        }
        if (c == 'e' || c == 'E') {
            add();
            if (c == '+' || c == '-') {
                add();
            }
            addDigits();
            isFloatingPoint = true;
        }
 
        String s = buf.toString();
        return isFloatingPoint 
            ? (length < 17) ? (Object)Double.valueOf(s) : new BigDecimal(s)
            : (length < 19) ? (Object)Long.valueOf(s) : new BigInteger(s);
    }
 
    protected int addDigits() {
        int ret;
        for (ret = 0; Character.isDigit(c); ++ret) {
            add();
        }
        return ret;
    }

    protected Object string() {
        buf.setLength(0);
        while (c != '"') {
            if (c == '\\') {
                next();
                if (c == 'u') {
                    add(unicode());
                } else {
                    Object value = escapes.get(Character.valueOf(c));
                    if (value != null) {
                        add(((Character) value).charValue());
                    }
                }
            } else {
                add();
            }
        }
        next();

        return buf.toString();
    }

    protected void add(char cc) {
        buf.append(cc);
        next();
    }

    protected void add() {
        add(c);
    }

    protected char unicode() {
        int value = 0;
        for (int i = 0; i < 4; ++i) {
            switch (next()) {
            case '0': case '1': case '2': case '3': case '4': 
            case '5': case '6': case '7': case '8': case '9':
                value = (value << 4) + c - '0';
                break;
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                value = (value << 4) + (c - 'a') + 10;
                break;
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                value = (value << 4) + (c - 'A') + 10;
                break;
            }
        }
        return (char) value;
    }
}