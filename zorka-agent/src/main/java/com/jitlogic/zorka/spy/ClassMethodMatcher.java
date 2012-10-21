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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ClassMethodMatcher {


    private String methodSignature;


    private Pattern classMatch, methodMatch, signatureMatch;


    public ClassMethodMatcher(String className, String methodName, String retType, String... argTypes) {
        this.classMatch = toSymbolMatch(className);
        this.methodMatch = toSymbolMatch(methodName);
        this.signatureMatch = toSignatureMatch(retType, argTypes);

    }

    private Pattern toSymbolMatch(String symbolName) {
        if (symbolName.startsWith("~")) {
            return Pattern.compile(symbolName.substring(1));
        } else {
            String s = symbolName.replaceAll("\\*\\*", "@A@").replaceAll("\\*", "@B@");
            return Pattern.compile(s.replaceAll("\\.", "\\\\.")
                    .replaceAll("@A@", ".+").replaceAll("@B@", "[a-zA-Z0-9_]+"));
        }
    }


    private static Map<String,String> stringMap(String...vals) {
        Map<String,String> map = new HashMap<String, String>(vals.length);

        for (int i = 1; i < vals.length; i += 2) {
            map.put(vals[i-1], vals[i]);
        }

        return Collections.unmodifiableMap(map);
    }


    private final static Map<String,String> typeCodes = stringMap(
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


    private final static Map<String,String> regexChars = stringMap(
        "[", "\\", "]", "\\]", ".", "\\.", ";", "\\;", "(", "\\(", ")", "\\)"
    );


    private static String strToRegex(String str) {
        StringBuilder sb = new StringBuilder(str.length()+32);
        for (char ch : str.toCharArray()) {
            sb.append(regexChars.containsKey(ch) ? regexChars.get(ch) : ch);
        }
        return sb.toString();
    }


    private Pattern toSignatureMatch(String retType, String...argTypes) {
        String retCode = retType != null ? strToRegex(toTypeCode(retType)) : ".*";
        StringBuilder sb = new StringBuilder(128);
        boolean moreAttrs = true;

        if (argTypes.length == 0) {
            sb.append("^\\(.*\\)");
        } else {
            sb.append("^\\(");
            for (String argType : argTypes) {
                if (SpyDefinition.NO_ARGS.equals(argType)) {
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


    public boolean matches(String clazzName) {
        return classMatch.matcher(clazzName).matches();
    }


    public boolean matches(String clazzName, String methodName) {
        return classMatch.matcher(clazzName).matches()
                && methodMatch.matcher(methodName).matches();
    }


    public boolean matches(String className, String methodName, String signature) {
        return classMatch.matcher(className).matches()
                && methodMatch.matcher(methodName).matches()
                && signatureMatch.matcher(signature).matches();
    }
}
