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

import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

import static com.jitlogic.zorka.spy.SpyLib.SM_NOARGS;

/**
 * This class is used to match class and method that have to be instrumented.
 * Spy matcher can use class patterns (names, ant-style masks or regexps),
 * method patterns, class annotation patterns and method annotation patterns.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SpyMatcher {

    /** Maps primitive types to type codes used by JVM */
    private static final Map<String,String> typeCodes = ZorkaUtil.constMap(
            "void", "V", "boolean", "Z", "byte", "B", "char", "C",
            "short", "S", "int", "I", "long", "J", "float", "F", "double", "D"
    );

    /** Maps regular expression special characters (sequences) to escaped sequences. */
    private static final Map<Character,String> regexChars = ZorkaUtil.constMap(
            '[', "\\]", ']', "\\]", '.', "\\.", ';', "\\;", '(', "\\(", ')', "\\)"
    );


    /** Access bits and custom matcher flags */
    private int access, flags;

    /** Flag that marks classPattern as class annotation rather than class name */
    public static final int CLASS_ANNOTATION  = 0x01;

    /** Flag that marks methodPattern as method annotation rather than method name */
    public static final int METHOD_ANNOTATION = 0x02;

    /** Special access bit - package private */
    public static final int ACC_PKGPRIV = 0x10000;

    /** No methods will regarding access bits */
    public static final int NULL_FILTER    = 0x0000;

    /** Default filter: public and package private methods match */
    public static final int DEFAULT_FILTER = ACC_PUBLIC|ACC_PKGPRIV;

    /** Public, private, protected and package private methods will match */
    public static final int ANY_FILTER     = ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED|ACC_PKGPRIV;

    /** Regexps for matching class name/annotation, method name/annotation and method descriptor */
    private Pattern classMatch, methodMatch, descriptorMatch;

    /**
     * Standard constructor
     *
     * @param flags custom matcher flags
     *
     * @param access java accessibility flags
     *
     * @param className class name/annotation pattern
     *
     * @param methodName method name/annotation pattern
     *
     * @param retType return type
     *
     * @param argTypes argument types
     */
    public SpyMatcher(int flags, int access, String className, String methodName, String retType, String... argTypes) {
        this.flags = flags;
        this.access = access;
        this.classMatch = toSymbolMatch(className);
        this.methodMatch = toSymbolMatch(methodName);
        this.descriptorMatch = toDescriptorMatch(retType, argTypes);
    }


    /**
     * Converts symbol match pattern (string) to regular expression object.
     *
     * @param symbolName symbol match pattern
     *
     * @return regular expression pattern
     */
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


    /**
     * Returns true if class of given name actually exists. As it prompts class loader
     * to load class, it shouldn't be used for anything else than standard JDK provided
     * classes.
     *
     * @param className full name (with package prefix)
     *
     * @return true if such class exists
     */
    private static boolean probeClass(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }


    /**
     * Converts java-like data type to JVM type string. Basic types and classes from java.lang
     * package can be passed in short form (eg. int, String). Other types have to be passed with
     * their full name (package "." className).
     *
     * @param type type (java language form)
     *
     * @return type descriptor (JVM form)
     */
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


    /**
     * Converts string to regular expression. Special characters will be automatically escaped.
     *
     * @param str string to be converted
     *
     * @return regular expression (still string form)
     */
    private static String strToRegex(String str) {
        StringBuilder sb = new StringBuilder(str.length()+32);
        for (char ch : str.toCharArray()) {
            sb.append(regexChars.containsKey(ch) ? regexChars.get(ch) : ch);
        }
        return sb.toString();
    }


    /**
     * Creates method descriptor from return type and argument types. Descriptor be a regexp
     * pattern matching strings in form '(argTypeDescriptors)retTypeDescriptor'.
     *
     * @param retType return type
     *
     * @param argTypes argument types
     *
     * @return regexp pattern suitable to match method descriptors
     */
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


    /**
     * Returns true if matcher contains class annotation (instead of class name pattern)
     *
     * @return true if matcher contains class annotation
     */
    public boolean hasClassAnnotation() {
        return 0 != (flags & CLASS_ANNOTATION);
    }


    /**
     * Returns true if matcher contains method annotation istead of method name pattern
     *
     * @return true or false
     */
    public boolean hasMethodAnnotation() {
        return 0 != (flags & METHOD_ANNOTATION);
    }


    /**
     * Returns true if any of candidate strings matches class pattern (be it single class name or
     * multiple class annotation names)
     *
     * @param classCandidates list of className candidates
     *
     * @return true if any candidate matches
     */
    public boolean matches(List<String> classCandidates) {
        for (String cm : classCandidates) {
            if (classMatch.matcher(cm).matches()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns true if given method annotation matches
     *
     * @param name methnod annotation name (full name with package prefix)
     *
     * @return true if METHOD_ANNOTATION flag is on and passed name matches methodPattern
     */
    public boolean matchMethodAnnotation(String name) {
        return hasMethodAnnotation() && methodMatch.matcher(name).matches();
    }


    /**
     * Returns true if any of candidate class names matches classPattern and methodName matches methodPattern.
     *
     * @param classCandidates list containing class name or names of class annotations
     *
     * @param methodName method name
     *
     * @return true or false
     */
    public boolean matches(List<String> classCandidates, String methodName) {
        return matches(classCandidates) && methodMatch.matcher(methodName).matches();
    }


    /**
     * Returns true if any of candudate class names matches classPattern, methodName matches methodPattern
     * and methodDescriptor matches method descriptor pattern.
     *
     * @param classCandidates list containing class name or names of class annotations
     *
     * @param methodName method name
     *
     * @param descriptor method descriptor (JVM form)
     *
     * @return true or false
     */
    public boolean matches(List<String> classCandidates, String methodName, String descriptor) {
        return matches(classCandidates, methodName) && descriptorMatch.matcher(descriptor).matches();
    }


    /**
     * Returns true if any of candidate class names matches classPattern, methodName matches methodPattern,
     * methodDescriptor matches method descriptor pattern and access flags match
     *
     * @param classCandidates list containing class name or names of class annotations
     *
     * @param methodName method name
     *
     * @param descriptor method descriptor (JVM form)
     *
     * @param access method access flags
     *
     * @return true or false
     */
    public boolean matches(List<String> classCandidates, String methodName, String descriptor, int access) {
        return matches(classCandidates, methodName, descriptor) && matches(access);
    }


    /**
     * Returns true if access flags match
     *
     * @param access method access flags
     *
     * @return true or false
     */
    private boolean matches(int access) {
        return this.access == 0 ||  (this.access & access) != 0;
    }
}
