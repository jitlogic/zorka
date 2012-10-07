/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.spy;


import bsh.This;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class defines mini-DSL for configuring instrumentator. Language allows for
 * choosing classes and methods to instrument, extracting parameters, return values,
 * transforming/filtering intercepted values and presenting them via JMX in various
 * ways.
 */
public class SpyDefinition {

    public static enum SpyType {
        INSTRUMENT,
        CATCH_ONCE,
        CATCH_EVERY;
    }

    public static final String NO_ARGS = "<no-args>";
    public static final String CONSTRUCTOR = ".ctor";

    private SpyType spyType;
    private Pattern classMatch, methodMatch, signatureMatch;
    private String methodSignature;


    public static SpyDefinition instrument(String classPattern, String methodPattern, String...signature) {
        return new SpyDefinition(SpyType.INSTRUMENT, classPattern, methodPattern, signature);
    }


    public static SpyDefinition catchOnce(String classPattern, String methodPattern, String...signature) {
        return new SpyDefinition(SpyType.CATCH_ONCE, classPattern, methodPattern, signature);
    }


    public static SpyDefinition catchEvery(String classPattern, String methodPattern, String...signature) {
        return new SpyDefinition(SpyType.CATCH_EVERY, classPattern, methodPattern, signature);
    }


    public SpyDefinition(SpyType spyType, String classPattern, String methodPattern, String...signature) {
        this.spyType = spyType;
        this.classMatch = toSymbolMatch(classPattern);
        this.methodMatch = toSymbolMatch(methodPattern);
        //this.signatureMatch = toSignatureMatch(argTypes);
        //this.methodSignature = argTypes;
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

    private Pattern toSignatureMatch(String...argTypes) {
        return null; // TODO
    }


    public DataCollector getCollector(String clazzName, String methodName, String methodSignature) {
        if (classMatch.matcher(clazzName).matches() &&
            methodMatch.matcher(methodName).matches() &&
            signatureMatch.matcher(methodSignature).matches()) {

            return null;
        }

        return null;
    }


    public boolean matches(String clazzName) {
        return classMatch.matcher(clazzName).matches();
    }



    public SpyDefinition withArguments(Integer... args) {
        return this;
    }


    public SpyDefinition withFormat(String...expr) {
        return this;
    }


    public SpyDefinition withRetVal() {
        return this;
    }

    public SpyDefinition withThread() {
        return this;
    }

    public SpyDefinition withClassLoader() {
        return this;
    }

    public SpyDefinition withClass(String className) {
        return this;
    }

    public SpyDefinition synchronizedWith(Integer arg) {
        return this;
    }

    public SpyDefinition filter(This ns, String func) {
        return this;
    }


    public SpyDefinition filter(int arg, String regex) {
        return this;
    }


    public SpyDefinition filterOut(int arg, String regex) {
        return this;
    }


    public SpyDefinition get(int arg, Object...args) {
        return this;
    }


    public SpyDefinition transform(int arg, String methodName, Object...args) {
        return this;
    }


    public SpyDefinition transform(This ns, String func) {
        return this;
    }


    public SpyDefinition onExit() {
        return this;
    }


    public SpyDefinition toStats(String mbsName, String beanName, String attrName) {
        return this;
    }


    public SpyDefinition toStats(String mbsName, String beanName, String attrName, String keyExpr) {
        return this;
    }


    public SpyDefinition toStat(String mbsName, String beanName, String attrName) {
        return this;
    }


    public SpyDefinition toAttr(String mbsName, String beanName, String attrName) {
        return this;
    }


    public SpyDefinition toBsh(This ns, String func) {
        return this;
    }


    public SpyDefinition toBsh(String ns, String func) {
        return this;
    }


    public SpyDefinition toCollector(DataCollector collector) {
        return this;
    }


    public SpyDefinition toGetter(String mbsName, String beanName, String attrName, Object...path) {
        return this;
    }
}
