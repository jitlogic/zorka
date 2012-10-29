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
import com.jitlogic.zorka.vmsci.SpySubmitter;
import com.jitlogic.zorka.spy.collectors.*;
import com.jitlogic.zorka.spy.transformers.*;

import java.util.*;

/**
 * This class defines mini-DSL for configuring instrumentation. Language allows for
 * choosing classes and methods to instrument, extracting parameters, return values,
 * transforming/filtering intercepted values and presenting them via JMX in various
 * ways.
 *
 * TODO better description of this class
 *
 */
public class SpyDefinition {

    public static final String NO_ARGS     = "<no-args>";
    public static final String CONSTRUCTOR = "<init>";
    public static final String ANY_TYPE    = null;
    public static final String STATIC      = "<clinit>";

    public static final int ON_ENTER   = 0;
    public static final int ON_EXIT    = 1;
    public static final int ON_ERROR   = 2;
    public static final int ON_SUBMIT  = 3;
    public static final int ON_COLLECT = 4;


    private static final List<SpyTransformer> EMPTY_XF =
            Collections.unmodifiableList(Arrays.asList(new SpyTransformer[0]));
    private static final List<SpyCollector> EMPTY_DC =
            Collections.unmodifiableList(Arrays.asList(new SpyCollector[0]));
    private static final List<SpyMatcher> EMPTY_MATCHERS =
            Collections.unmodifiableList(Arrays.asList(new SpyMatcher[0]));
    private static final List<SpyProbeElement> EMPTY_AF =
            Collections.unmodifiableList(Arrays.asList(new SpyProbeElement[0]));


    private List<SpyProbeElement>[] probes;
    private List<SpyTransformer>[] transformers;

    private List<SpyCollector> collectors = EMPTY_DC;
    private List<SpyMatcher> matchers = EMPTY_MATCHERS;

    private int curStage = ON_ENTER;
    private boolean once = false;

    public static SpyDefinition instrument() {
        return new SpyDefinition().withTime().onExit().withTime().onError().withTime().onEnter();
    }

    public static SpyDefinition newInstance() {
        return new SpyDefinition();
    }

    private SpyDefinition() {

        probes = new List[5];
        for (int i = 0; i < probes.length; i++) {
            probes[i] = EMPTY_AF;
        }

        transformers = new List[5];
        for (int i = 0; i < transformers.length; i++) {
            transformers[i] = EMPTY_XF;
        }
    }


    private SpyDefinition(SpyDefinition orig) {
        this.matchers = orig.matchers;
        this.probes = Arrays.copyOf(orig.probes, orig.probes.length);
        this.transformers = Arrays.copyOf(orig.transformers, orig.transformers.length);
        this.collectors = orig.collectors;
    }


    /**
     * Returns list of probe definitions from particular stage
     *
     * @param stage stage we're interested in
     *
     * @return list of probes defined for this stage
     */
    public List<SpyProbeElement> getProbes(int stage) {
        return probes[stage];
    }


    /**
     * Returns list of transformers for a particular stage.
     *
     * @param stage
     *
     * @return
     */
    public List<SpyTransformer> getTransformers(int stage) {
        return transformers[stage];
    }


    /**
     * Returns list of submitters definitions.
     *
     * @return
     */
    public List<SpyCollector> getCollectors() {
        return collectors;
    }


    /**
     * Returns true if given class name matches this spy definition.
     *
     * @param className
     *
     * @return
     */
    public boolean match(String className) {

        for (SpyMatcher matcher : matchers) {
            if (matcher.matches(className)) {
                return true;
            }
        }

        return false;
    }

    public boolean match(String className, String methodName, String methodDesc, int access) {

        for (SpyMatcher matcher : matchers) {
            if (matcher.matches(className, methodName, methodDesc, access)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns list of matchers declared SpyDefinition.
     *
     * @return list of matchers
     */
    public List<SpyMatcher> getMatchers() {
        return matchers;
    }


    public boolean isOnce() {
        return once;
    }

    /**
     * Instructs spy what should be collected at the beginning of a method.
     *
     * @return
     */
    public SpyDefinition onEnter() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = ON_ENTER;
        return sdef;
    }


    /**
     * Instructs spy what should be collected at the end of a method.
     *
     * @return
     */
    public SpyDefinition onExit() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = ON_EXIT;
        return sdef;
    }


    /**
     * Instructs spy what should be collected at exception handling code of a method.
     *
     * @return
     */
    public SpyDefinition onError() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = ON_ERROR;
        return sdef;
    }


    /**
     * Instructs spy that subsequent transforms will be executed at data submission
     * point.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onSubmit() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = ON_SUBMIT;
        return sdef;
    }


    /**
     * Instructs spy that subsequent transforms will be executed jest before passing
     * data to collector objects. Transforms execution at this point is guaranteed to
     * be single threaded and can execute asynchronously to instrumented methods.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onCollect() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = ON_COLLECT;
        return sdef;
    }

    /**
     * Instructs spy what methods (of what classes) this instrumentation
     * definition should be applied on.
     *
     * @param classPattern
     *
     * @param methodPattern
     *
     * @return
     */
    public SpyDefinition lookFor(String classPattern, String methodPattern) {
        return lookFor(classPattern, methodPattern, null, SpyMatcher.DEFAULT_FILTER);
    }


    /**
     * Instructs spy what methods (of what classes) this instrumentation
     * definition should be applied on.
     *
     * @param classPattern
     *
     * @param methodPattern
     *
     * @param retType
     *
     * @param flags
     *
     * @param argTypes
     *
     * @return
     */
    public SpyDefinition lookFor(String classPattern, String methodPattern, String retType, int flags, String...argTypes) {
        SpyDefinition sdef = new SpyDefinition(this);
        List<SpyMatcher> lst = new ArrayList<SpyMatcher>(sdef.matchers.size()+1);
        lst.addAll(sdef.matchers);
        lst.add(new SpyMatcher(classPattern, methodPattern, retType, flags, argTypes));
        sdef.matchers = lst;
        return sdef;
    }


    public SpyDefinition once() {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.once = true;
        return sdef;
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
     * @param args argument indexes to be fetched (or class names if
     *             class objects are to be fetched)
     *
     * @return spy definition with augmented fetched argument list;
     */
    public SpyDefinition withArguments(Object... args) {
        SpyDefinition sdef = new SpyDefinition(this);

        List<SpyProbeElement> lst = new ArrayList<SpyProbeElement>(sdef.probes[curStage].size()+args.length);
        lst.addAll(sdef.probes[curStage]);
        for (Object arg : args) {
            lst.add(new SpyProbeElement(arg));
        }

        sdef.probes = Arrays.copyOf(probes, probes.length);
        sdef.probes[curStage] = Collections.unmodifiableList(lst);

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
    public SpyDefinition withFormat(int dst, String expr) {
        return withTransformer(new StringFormatTransformer(dst, expr));
    }


    /**
     * Fetches current time at current stage of execution.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withTime() {
        return this.withArguments(SpyProbeElement.FETCH_TIME);
    }


    /**
     * Instructs spy that return value should also be catched.
     * Return value will be added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withRetVal() {
        return this.withArguments(SpyProbeElement.FETCH_RET_VAL);
    }


    /**
     * Fetches exception
     * @return
     */
    public SpyDefinition withError() {
        return this.withArguments(SpyProbeElement.FETCH_ERROR);
    }

    /**
     * Instructs spy that current thread should be catched.
     * Reference to current thread will be added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withThread() {
        return this.withArguments(SpyProbeElement.FETCH_THREAD);
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
     * Adds a custom transformer to transform chain.
     *
     * @param transformer
     *
     * @return
     */
    public SpyDefinition withTransformer(SpyTransformer transformer) {
        SpyDefinition sdef = new SpyDefinition(this);
        List<SpyTransformer> lst = new ArrayList<SpyTransformer>(transformers[curStage].size()+1);
        lst.addAll(transformers[curStage]);
        lst.add(transformer);
        sdef.transformers = Arrays.copyOf(transformers, transformers.length);
        sdef.transformers[curStage] = lst;
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
     * Instructs spy to submit data to a given submitter.
     *
     * @param collector
     *
     * @return augmented spy definition
     */
    public SpyDefinition toCollector(SpyCollector collector) {
        SpyDefinition sdef = new SpyDefinition(this);
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
