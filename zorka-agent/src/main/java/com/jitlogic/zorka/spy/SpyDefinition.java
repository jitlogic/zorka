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


import bsh.This;
import com.jitlogic.zorka.spy.collectors.*;
import com.jitlogic.zorka.spy.transformers.*;

import java.util.*;

/**
 * This class defines mini-DSL for configuring instrumentator. Language allows for
 * choosing classes and methods to instrument, extracting parameters, return values,
 * transforming/filtering intercepted values and presenting them via JMX in various
 * ways.
 *
 * TODO better description of this class
 *
 */
public class SpyDefinition {

    public static enum SpyType {
        INSTRUMENT,
        CATCH_ONCE,
        CATCH_EVERY;
    }

    public static final String NO_ARGS = "<no-args>";
    public static final String CONSTRUCTOR = "<init>";
    public static final String ANY_TYPE = null;  // currently it only applies to return type
    public static final String STATIC = "<clinit>";

    private static final Integer FETCH_RET_VAL = -1;
    private static final Integer FETCH_THREAD  = -2;
    private static final Integer FETCH_LOADER  = -3;

    private SpyType spyType;
    private ClassMethodMatcher matcher;
    private boolean onExit = false;

    private static final List<Object> EMPTY_OBJS = Collections.unmodifiableList(Arrays.asList(new Object[0]));
    private static final List<ArgTransformer> EMPTY_XF = Collections.unmodifiableList(Arrays.asList(new ArgTransformer[0]));
    private static final List<SpyCollector> EMPTY_DC = Collections.unmodifiableList(Arrays.asList(new SpyCollector[0]));

    private List<Object> fetchArgs = EMPTY_OBJS;
    private List<String> formatStrings;

    private Integer synchronizeWithArg;
    private List<Object> synchronizeWithPath;
    private Object synchronizeWithObj;

    private List<ArgTransformer> transformers = EMPTY_XF;
    private List<SpyCollector> collectors = EMPTY_DC;

    public static SpyDefinition instrument(String classPattern, String methodPattern, String retType, String...argTypes) {
        return new SpyDefinition(SpyType.INSTRUMENT, classPattern, methodPattern, retType, argTypes);
    }


    public static SpyDefinition catchOnce(String classPattern, String methodPattern, String retType, String...argTypes) {
        return new SpyDefinition(SpyType.CATCH_ONCE, classPattern, methodPattern, retType, argTypes);
    }


    public static SpyDefinition catchEvery(String classPattern, String methodPattern, String retType, String...argTypes) {
        return new SpyDefinition(SpyType.CATCH_EVERY, classPattern, methodPattern, retType, argTypes);
    }


    public SpyDefinition(SpyType spyType, String classPattern, String methodPattern, String retType, String...argTypes) {
        this.spyType = spyType;
        matcher = new ClassMethodMatcher(classPattern, methodPattern, retType, argTypes);
    }


    private SpyDefinition fullClone() {
        // TODO implement actual cloning
        return this;
    }


    public DataCollector getCollector(String clazzName, String methodName, String methodSignature) {
//        if (matcher.matcher(clazzName).matches() &&
//            methodMatch.matcher(methodName).matches() &&
//            signatureMatch.matcher(methodSignature).matches()) {
//
//            return null;
//        }

        return null;
    }


    public ClassMethodMatcher getMatcher() {
        return matcher;
    }


    public SpyType getType() {
        return spyType;
    }


    /**
     * Declares which arguments should be fetched by instrumenting code.
     * This method should generally be called once as arguments are fetched
     * only at one place of a method (beginning or end - depending on
     * whether instrumentation will actually run at the beginning or at the end of
     * method.
     *
     * <p>For instance methods first argument will have index 1 and
     *    instance reference will be present at index 0. </p>
     * <p>For static methods arguments start with 0. </p>
     *
     * @param args argument indexes to be fetched
     *
     * @return spy definition with augmented fetched argument list;
     */
    public SpyDefinition withArguments(Object... args) {
        SpyDefinition sdef = this.fullClone();

        List<Object> lst = new ArrayList<Object>(sdef.fetchArgs.size()+args.length);
        lst.addAll(sdef.fetchArgs);
        lst.addAll(Arrays.asList(args));

        sdef.fetchArgs = Collections.unmodifiableList(lst);
        return sdef;
    }


    /**
     * Formats arguments and passes an array of formatted strings.
     * Format expression is generally a string with special marker for
     * extracting previous arguments '{n.field1.field2...}' where n is argument
     * number, field1,field2,... are (optional) fields used exactly as in
     * zorkalib.get() function.
     *
     * @param expr format expressions.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withFormat(String...expr) {
        SpyDefinition sdef = this.fullClone();
        sdef.formatStrings = Collections.unmodifiableList(Arrays.asList(expr));
        return sdef;
    }


    /**
     * Instructs spy that return value should also be catched.
     * Return value will be added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withRetVal() {
        return this.withArguments(FETCH_RET_VAL);
    }


    /**
     * Instructs spy that current thread should be catched.
     * Reference to current thread will be added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withThread() {
        return this.withArguments(FETCH_THREAD);
    }


    /**
     * Instructs spy that current class loader should be catched.
     * Reference to current class loader will added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withClassLoader() {
        return this.withArguments(FETCH_LOADER);
    }


    /**
     * Instructs spy that a given class reference should be catched.
     * Reference to the class will be added at the end of current argument list.
     *
     * @param className full class name (along with package)
     *
     * @return augmented spy definition
     */
    public SpyDefinition withClass(String className) {
        return this.withArguments(className);
    }


    /**
     * Synchronize with object given by argument numbered as in withArgs().
     *
     * @param arg argumetn index
     *
     * @return augmented spy definition
     */
    public SpyDefinition synchronizeWithArg(Integer arg, Object...path) {
        SpyDefinition sdef = this.fullClone();
        sdef.synchronizeWithArg = arg;
        sdef.synchronizeWithPath = Collections.unmodifiableList(Arrays.asList(path));
        return sdef;
    }


    /**
     * Synchronize with an specific object passed as obj.
     *
     * @param obj object we will synchronize with
     *
     * @return augmented spy definition
     */
    public SpyDefinition synchronizeWithObj(Object obj) {
        SpyDefinition sdef = this.fullClone();
        sdef.synchronizeWithObj = obj;
        return sdef;
    }


    /**
     * Adds a custom transformer to transform chain.
     *
     * @param transformer
     *
     * @return
     */
    public SpyDefinition withTransformer(ArgTransformer transformer) {
        SpyDefinition sdef = this.fullClone();
        List<ArgTransformer> lst = new ArrayList<ArgTransformer>(transformers.size()+1);
        lst.addAll(transformers);
        lst.add(transformer);
        sdef.transformers = lst;
        return sdef;
    }


    /**
     * Add an BSH filtering transformer to transform chain.
     *
     * @param ns BSH namespace
     *
     * @param func BSH function name
     *
     * @return augmented spy definition
     */
    public SpyDefinition filter(This ns, String func) {
        return withTransformer(new BshFilterTransformer(ns, func));
    }


    /**
     * Add an regex filtering transformer to transform chain.
     *
     * @param arg argument number
     *
     * @param regex regular expression
     *
     * @return augmented spy definition
     */
    public SpyDefinition filter(Integer arg, String regex) {
        return withTransformer(new RegexFilterTransformer(arg, regex));
    }


    /**
     * Add an regex filtering transformer to transform chain that will exclude
     * matching items.
     *
     * @param arg argument number
     *
     * @param regex regular expression
     *
     * @return augmented spy definition
     */
    public SpyDefinition filterOut(Integer arg, String regex) {
        return withTransformer(new RegexFilterTransformer(arg, regex, true));
    }


    /**
     * Gets slot number n, performs traditional get operation and stores
     * results in the same slot.
     *
     * @param arg
     *
     * @param path
     *
     * @return augmented spy definition
     */
    public SpyDefinition get(int arg, Object...path) {
        return withTransformer(new GetterTransformer(arg, arg, path));
    }


    /**
     * Gets slot number arg, performs traditional get operation and stores
     * results in other slot.
     *
     * @param arg
     *
     * @param dst
     *
     * @param path
     *
     * @return augmented spy definition
     */
    public SpyDefinition getTo(int arg, int dst, Object...path) {
        return withTransformer(new GetterTransformer(arg, arg, path));
    }


    /**
     * Gets object from slot number arg, calls given method on this slot and
     * if method returns some value, stores its result in this slot.
     *
     * @param arg
     *
     * @param methodName
     *
     * @param methodArgs
     *
     * @return augmented spy definition
     */
    public SpyDefinition transform(int arg, String methodName, Object...methodArgs) {
        return withTransformer(new MethodCallingTransformer(arg, arg, methodName, methodArgs));
    }


    /**
     * Gets object from slot number arg, calls given method on this slot and
     * if method returns some value, stores its result in this slot.
     *
     * @param src
     *
     * @param dst
     *
     * @param methodName
     *
     * @param methodArgs
     *
     * @return augmented spy definition
     */
    public SpyDefinition transformTo(int src, int dst, String methodName, Object...methodArgs) {
        return withTransformer(new MethodCallingTransformer(src, dst, methodName, methodArgs));
    }

    /**
     * Transforms whole argument array using BSH function.
     *
     * @param ns BSH namespace
     *
     * @param func function name
     *
     * @return augmented spy definition
     */
    public SpyDefinition transform(This ns, String func) {
        return withTransformer(new BshFunctionTransformer(ns, func));
    }


    /**
     * Instructs spy that arguments will be grabbed and processed on exit.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onExit() {
        SpyDefinition sdef = this.fullClone();
        sdef.onExit = true;
        return sdef;
    }


    /**
     * Instructs spy to submit data to a given collector.
     *
     * @param collector
     *
     * @return augmented spy definition
     */
    public SpyDefinition toCollector(SpyCollector collector) {
        SpyDefinition sdef = this.fullClone();
        List<SpyCollector> lst = new ArrayList<SpyCollector>(collectors.size()+1);
        lst.addAll(collectors); lst.add(collector);
        sdef.collectors = lst;
        return sdef;
    }


    /**
     * Instructs spy to submit data to traditional Zorka Statistics object. Statistics
     * will be organized by method name.
     *
     * @param mbsName mbean server name
     *
     * @param beanName bean name
     *
     * @param attrName attribute name
     *
     * @return augmented spy definition
     */
    public SpyDefinition toStats(String mbsName, String beanName, String attrName) {
        return toCollector(new ZorkaStatsCollector(mbsName, beanName, attrName, null));
    }


    /**
     * Instructs spy to submit data to traditional Zorka Statistics object. Statistics
     * will be organized by keyExpr having all its macros properly expanded.
     *
     * @param mbsName mbean server name
     *
     * @param beanName bean name
     *
     * @param attrName attribute name
     *
     * @param keyExpr key expression
     *
     * @return augmented spy definition
     */
    public SpyDefinition toStats(String mbsName, String beanName, String attrName, String keyExpr) {
        return toCollector(new ZorkaStatsCollector(mbsName, beanName, attrName, keyExpr));
    }


    /**
     * Instructs spy to submit data to a single object of ZorkaStat type and present
     * it as Zorka
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @return augmented spy definition
     */
    public SpyDefinition toStat(String mbsName, String beanName, String attrName) {
        return toCollector(new JmxAttrCollector(mbsName, beanName, attrName));
    }


    /**
     * Instructs spy to submit data to a single object of ZorkaStat type and presents
     * selected attribute of this object.
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @param statAttr which stat attr to present
     *
     * @return augmented spy definition
     */
    public SpyDefinition toStatAttr(String mbsName, String beanName, String attrName, String statAttr) {
        return toCollector(new JmxAttrCollector(mbsName, beanName, attrName, statAttr));
    }


    /**
     * Instructs spy to submit data to a single BSH function.
     *
     * @param ns BSH namespace
     *
     * @param func function name
     *
     * @return augmented spy definition
     */
    public SpyDefinition toBsh(This ns, String func) {
        return toCollector(new BshFuncCollector(ns, func));
    }


    /**
     * Instructs spy to submit data to a single BSH function.
     *
     * @param ns BSH namespace
     *
     * @param func function name
     *
     * @return augmented spy definition
     */
    public SpyDefinition toBsh(String ns, String func) {
        return toCollector(new BshFuncCollector(ns, func));
    }


    /**
     * Instructs spy to submit data to a single BSH function.
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @param path which stat attr to present
     *
     * @return augmented spy definition
     */
    public SpyDefinition toGetter(String mbsName, String beanName, String attrName, Object...path) {
        return toCollector(new GetterPresentingCollector(mbsName, beanName, attrName, path));
    }
}
