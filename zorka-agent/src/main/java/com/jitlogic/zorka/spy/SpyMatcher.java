/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;

import java.util.*;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

import static com.jitlogic.zorka.spy.SpyLib.SM_NOARGS;

public class SpyMatcher {

    private String methodSignature;
    private int access, flags;

    public static final int CLASS_ANNOTATION  = 0x01;
    public static final int METHOD_ANNOTATION = 0x02;

    public static final int ACC_PKGPRIV = 0x10000;

    public static final int NULL_FILTER    = 0x0000;
    public static final int DEFAULT_FILTER = ACC_PUBLIC|ACC_PKGPRIV;
    public static final int ANY_FILTER     = ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED|ACC_PKGPRIV;

    private Pattern classMatch, methodMatch, descriptorMatch;

    public SpyMatcher(int flags, int access, String className, String methodName, String retType, String... argTypes) {
        this.flags = flags;
        this.access = access;
        this.classMatch = toSymbolMatch(className);
        this.methodMatch = toSymbolMatch(methodName);
        this.descriptorMatch = toDescriptorMatch(retType, argTypes);
    }


    private Pattern toSymbolMatch(String symbolName) {
        if (symbolName == null) {
            return Pattern.compile(".*");
        } else if (symbolName.startsWith("~")) {
            return Pattern.compile(symbolName.substring(1));
        } else {
            return Pattern.compile(symbolName.replaceAll("\\.", "\\\\.")
                    .replaceAll("\\*\\*", ".+").replaceAll("\\*", "[a-zA-Z0-9_]+"));
        }
    }


    private static Map<String,String> stringMap(String...vals) {
        Map<String,String> map = new HashMap<String, String>(vals.length);

        for (int i = 1; i < vals.length; i += 2) {
            map.put(vals[i-1], vals[i]);
        }

        return Collections.unmodifiableMap(map);
    }


    private static final Map<String,String> typeCodes = stringMap(
        "void", "V",   "boolean", "Z","byte", "B",   "char", "C",
        "short", "S",  "int", "I", "long", "J", "float", "F", "double", "D"
    );


    private static boolean probeClass(String className) {
        try {
            return Class.forName(className) != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    private static String toTypeCode(String type) {
        StringBuilder sb = new StringBuilder();

        while (type.endsWith("[]")) {
            sb.append("[");
            type = type.substring(0, type.length()-2);
        }

        if (typeCodes.containsKey(type)) {
            sb.append(typeCodes.get(type));
        } else if (type.contains(".")) {
            sb.append('L');
            sb.append(type.replace('.', '/'));
            sb.append(";");
        } else if (probeClass("java.lang." + type)) {
            sb.append('L');
            sb.append("java/lang/" + type);
            sb.append(";");
        }

        return sb.toString();
    }


    private static final Map<String,String> regexChars = stringMap(
        "[", "\\", "]", "\\]", ".", "\\.", ";", "\\;", "(", "\\(", ")", "\\)"
    );


    private static String strToRegex(String str) {
        StringBuilder sb = new StringBuilder(str.length()+32);
        for (char ch : str.toCharArray()) {
            sb.append(regexChars.containsKey(ch) ? regexChars.get(ch) : ch);
        }
        return sb.toString();
    }


    private Pattern toDescriptorMatch(String retType, String... argTypes) {
        String retCode = retType != null ? strToRegex(toTypeCode(retType)) : ".*";
        StringBuilder sb = new StringBuilder(128);
        boolean moreAttrs = true;

        if (argTypes.length == 0) {
            sb.append("^\\(.*\\)");
        } else {
            sb.append("^\\(");
            for (String argType : argTypes) {
                if (SM_NOARGS.equals(argType)) {
                    moreAttrs = false;
                    break;
                }
                sb.append(strToRegex(toTypeCode(argType)));
            }
            sb.append(moreAttrs ? ".*\\)" : "\\)");
        }

        sb.append(retCode);
        sb.append("$");

        return Pattern.compile(sb.toString());
    }


    public boolean hasClassAnnotation() {
        return 0 != (flags & CLASS_ANNOTATION);
    }


    public boolean hasMethodAnnotation() {
        return 0 != (flags & METHOD_ANNOTATION);
    }


    public boolean matches(List<String> classCandidates) {
        for (String cm : classCandidates) {
            if (classMatch.matcher(cm).matches()) {
                return true;
            }
        }
        return false;
    }


    public boolean matchMethodAnnotation(String name) {
        return hasMethodAnnotation() && methodMatch.matcher(name).matches();
    }


    public boolean matches(List<String> classCandidates, String methodName) {
        return matches(classCandidates) && methodMatch.matcher(methodName).matches();
    }


    public boolean matches(List<String> classCandidates, String methodName, String descriptor) {
        return matches(classCandidates, methodName) && descriptorMatch.matcher(descriptor).matches();
    }


    public boolean matches(List<String> classCandidates, String methodName, String descriptor, int access) {
        return matches(classCandidates, methodName, descriptor) && matches(access);
    }


    private boolean matches(int access) {
        return this.access == 0 ||
          0 != (this.access & (ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED)) ?
            (0 != (access & this.access)) : (0 != (this.access & ACC_PKGPRIV));
    }
}
