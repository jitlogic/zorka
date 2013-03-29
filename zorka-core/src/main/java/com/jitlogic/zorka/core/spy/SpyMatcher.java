/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.core.util.ZorkaUtil;

import java.util.*;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

import static com.jitlogic.zorka.core.spy.SpyLib.SM_NOARGS;

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


    /** List of common methods (typically omitted by instrumentation) */
    public static final Set<String> COMMON_METHODS = ZorkaUtil.set("toString", "equals", "hashCode", "valueOf");


    public static final int BY_CLASS_NAME        = 0x001;
    public static final int BY_CLASS_ANNOTATION  = 0x002;
    public static final int BY_INTERFACE         = 0x004;
    public static final int BY_METHOD_NAME       = 0x008;
    public static final int BY_METHOD_SIGNATURE  = 0x010;
    public static final int BY_METHOD_ANNOTATION = 0x020;
    public static final int NO_CONSTRUCTORS      = 0x040;
    public static final int NO_ACCESSORS         = 0x080;
    public static final int NO_COMMONS           = 0x100;
    public static final int EXCLUDE_MATCH        = 0x800;

    /** Special access bit - package private */
    public static final int ACC_PKGPRIV = 0x10000;

    /** No methods will regarding access bits */
    public static final int NULL_FILTER = 0x0000;

    /** Default filter: public and package private methods match */
    public static final int DEFAULT_FILTER = ACC_PUBLIC|ACC_PKGPRIV;

    /** Public, private, protected and package private methods will match */
    public static final int ANY_FILTER     = ACC_PUBLIC|ACC_PRIVATE|ACC_PROTECTED|ACC_PKGPRIV;

    /** Access bits and custom matcher flags */
    private int access, flags;

    /** Regexps for matching class name/annotation, method name/annotation and method descriptor */
    private Pattern classPattern, methodPattern, signaturePattern;


    /**
     * Creates spy matcher
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
        this.classPattern = toSymbolMatch(className);
        this.methodPattern = toSymbolMatch(methodName);
        this.signaturePattern = toDescriptorMatch(retType, argTypes);
    }


    /**
     * Copy constructor (for trace alteration methods below)
     *
     * @param flags custom matcher flags
     *
     * @param access accessibility flags
     *
     * @param classPattern class name/annotation pattern
     *
     * @param methodPattern method name/annotation pattern
     *
     * @param signaturePattern signature pattern
     */
    private SpyMatcher(int flags, int access, Pattern classPattern, Pattern methodPattern, Pattern signaturePattern) {
        this.access = access;
        this.flags = flags;
        this.classPattern = classPattern;
        this.methodPattern = methodPattern;
        this.signaturePattern = signaturePattern;
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


    public int getFlags() {
        return flags;
    }


    public int getAccess() {
        return access;
    }


    public Pattern getClassPattern() {
        return classPattern;
    }


    public Pattern getMethodPattern() {
        return methodPattern;
    }


    public Pattern getSignaturePattern() {
        return signaturePattern;
    }


    /**
     * Marks trace as inverted trace (matching methods will be excluded rathern than included).
     *
     * @return altered spy matcher
     */
    public SpyMatcher exclude() {
        return new SpyMatcher(flags | EXCLUDE_MATCH, access, classPattern, methodPattern, signaturePattern);
    }


    /**
     * Excludes constructors and static constructors.
     *
     * @return altered spy matcher
     */
    public SpyMatcher noConstructors() {
        return new SpyMatcher(flags | NO_CONSTRUCTORS, access, classPattern, methodPattern, signaturePattern);
    }


    /**
     * Excludes accessor methods (that is, getters and setters)
     *
     * @return altered spy matcher
     */
    public SpyMatcher noAccessors() {
        return new SpyMatcher(flags | NO_ACCESSORS, access, classPattern, methodPattern, signaturePattern);
    }


    /**
     * Excludes some commony used methods, like equals(), toString(), hashCode() etc.
     *
     * @return altered spy matcher
     */
    public SpyMatcher noCommons() {
        return new SpyMatcher(flags | NO_COMMONS, access, classPattern, methodPattern, signaturePattern);
    }

    /**
     * Excludes typical methods not suitable for tracer (constructors, accessors, some common methods).
     *
     * @return altered spy matcher
     */
    public SpyMatcher forTrace() {
        return new SpyMatcher(flags|NO_CONSTRUCTORS|NO_ACCESSORS|NO_COMMONS, access, classPattern, methodPattern, signaturePattern);
    }
}
