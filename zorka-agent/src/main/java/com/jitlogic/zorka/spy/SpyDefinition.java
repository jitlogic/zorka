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
import com.jitlogic.zorka.agent.FileTrapper;
import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.snmp.SnmpTrapper;
import com.jitlogic.zorka.integ.snmp.TrapVarBindDef;
import com.jitlogic.zorka.integ.syslog.SyslogTrapper;
import com.jitlogic.zorka.integ.zabbix.ZabbixTrapper;
import com.jitlogic.zorka.spy.collectors.*;
import com.jitlogic.zorka.spy.processors.*;
import com.jitlogic.zorka.util.ZorkaLogLevel;
import com.jitlogic.zorka.util.ZorkaUtil;
import com.jitlogic.zorka.normproc.Normalizer;

import java.util.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

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

    private static final List<SpyProcessor> EMPTY_XF =
            Collections.unmodifiableList(Arrays.asList(new SpyProcessor[0]));
    private static final List<SpyCollector> EMPTY_DC =
            Collections.unmodifiableList(Arrays.asList(new SpyCollector[0]));
    private static final List<SpyMatcher> EMPTY_MATCHERS =
            Collections.unmodifiableList(Arrays.asList(new SpyMatcher[0]));
    private static final List<SpyProbeElement> EMPTY_AF =
            Collections.unmodifiableList(Arrays.asList(new SpyProbeElement[0]));

    private List<SpyProbeElement>[] probes;
    private List<SpyProcessor>[] processors;

    private List<SpyCollector> collectors = EMPTY_DC;
    private List<SpyMatcher> matchers = EMPTY_MATCHERS;

    private int curStage = ON_ENTER;
    private boolean once = false;

    public static SpyDefinition instrument() {
        return new SpyDefinition().withTime().onReturn().withTime().onError().withTime().onEnter();
    }

    public static SpyDefinition instance() {
        return new SpyDefinition();
    }

    public SpyDefinition() {

        probes = new List[5];
        for (int i = 0; i < probes.length; i++) {
            probes[i] = EMPTY_AF;
        }

        processors = new List[5];
        for (int i = 0; i < processors.length; i++) {
            processors[i] = EMPTY_XF;
        }
    }


    private SpyDefinition(SpyDefinition orig) {
        this.probes = ZorkaUtil.copyArray(orig.probes);
        this.processors = ZorkaUtil.copyArray(orig.processors);
        this.collectors = orig.collectors;
        this.matchers = orig.matchers;
        this.curStage = orig.curStage;
        this.once = orig.once;
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
     * Returns list of processors for a particular stage.
     *
     * @param stage
     *
     * @return
     */
    public List<SpyProcessor> getProcessors(int stage) {
        return processors[stage];
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
     * Returns true if given class name matches this sdef.
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


    /**
     * Returns true if given method (of given class) matches this spy definition.
     * Note that method signature and access bits are also checked.
     *
     * @param className class name
     *
     * @param methodName method name
     *
     * @param methodDesc method descriptor (as in classfile)
     *
     * @param access access bits (as in classfile)
     *
     * @return true if all arguments match properly.
     */
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
     *
     * @param stage
     * @return
     */
    public SpyDefinition on(int stage) {
        SpyDefinition sdef = new SpyDefinition(this);
        sdef.curStage = stage;
        return sdef;
    }

    /**
     * Instructs spy what should be collected at the beginning of a method.
     *
     * @return
     */
    public SpyDefinition onEnter() {
        return on(ON_ENTER);
    }


    /**
     * Instructs spy what should be collected at the end of a method.
     *
     * @return
     */
    public SpyDefinition onReturn() {
        return on(ON_RETURN);
    }


    /**
     * Instructs spy what should be collected at exception handling code of a method.
     *
     * @return
     */
    public SpyDefinition onError() {
        return on(ON_ERROR);
    }


    /**
     * Instructs spy that subsequent transforms will be executed at data submission
     * point.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onSubmit() {
        return on(ON_SUBMIT);
    }


    /**
     * Instructs spy that subsequent transforms will be executed jest before passing
     * data to collector objects. Transforms execution at this point is guaranteed to
     * be single threaded and can execute asynchronously to instrumented methods.
     *
     * @return augmented spy definition
     */
    public SpyDefinition onCollect() {
        return on(ON_COLLECT);
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
    public SpyDefinition include(String classPattern, String methodPattern) {
        return include(SpyMatcher.DEFAULT_FILTER, classPattern, methodPattern, null);
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
     * @param access
     *
     * @param argTypes
     *
     * @return
     */
    public SpyDefinition include(int access, String classPattern, String methodPattern, String retType, String... argTypes) {
        SpyDefinition sdef = new SpyDefinition(this);
        List<SpyMatcher> lst = new ArrayList<SpyMatcher>(sdef.matchers.size()+2);
        lst.addAll(sdef.matchers);
        lst.add(new SpyMatcher(classPattern, methodPattern, retType, access, argTypes));
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

        List<SpyProbeElement> lst = new ArrayList<SpyProbeElement>(sdef.probes[curStage].size()+args.length+2);
        lst.addAll(sdef.probes[curStage]);
        for (Object arg : args) {
            lst.add(new SpyProbeElement(arg));
        }

        sdef.probes = ZorkaUtil.copyArray(probes);
        sdef.probes[curStage] = Collections.unmodifiableList(lst);

        return sdef;
    }


    /**
     * Fetches current time at current stage of execution.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withTime() {
        return this.withArguments(FETCH_TIME);
    }




    /**
     * Instructs spy that return value should also be catched.
     * Return value will be added at the end of current argument list.
     *
     * @return augmented spy definition
     */
    public SpyDefinition withRetVal() {
        return this.withArguments(FETCH_RETVAL);
    }


    /**
     * Fetches exception
     * @return
     */
    public SpyDefinition withError() {
        return this.withArguments(FETCH_ERROR);
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
     * Instruct spy to fetch null value. This is useful when you want
     * to force spy to insert any probe but no actual values are needed.
     *
     * @return
     */
    public SpyDefinition withNull() {
        return this.withArguments(FETCH_NULL);
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
     * Adds a custom transformer to process chain.
     *
     * @param processor
     *
     * @return
     */
    public SpyDefinition withProcessor(SpyProcessor processor) {
        SpyDefinition sdef = new SpyDefinition(this);
        List<SpyProcessor> lst = new ArrayList<SpyProcessor>(processors[curStage].size()+2);
        lst.addAll(processors[curStage]);
        lst.add(processor);
        sdef.processors = ZorkaUtil.copyArray(processors);
        sdef.processors[curStage] = lst;
        return sdef;
    }

    /**
     * Formats arguments and passes an array of formatted strings.
     * Format expression is generally a string with special marker for
     * extracting previous arguments '${n.field1.field2...}' where n is argument
     * number, field1,field2,... are (optional) fields used exactly as in
     * zorkalib.get() function.
     *
     * @param expr format expressions.
     *
     * @return augmented spy definition
     */
    public SpyDefinition format(int dst, String expr) {
        return withProcessor(new StringFormatProcessor( dst, expr));
    }



    /**
     * Add an regex filtering transformer to process chain.
     *
     * @param src argument number
     *
     * @param regex regular expression
     *
     * @return augmented spy definition
     */
    public SpyDefinition filter(int src, String regex) {
        return withProcessor(new RegexFilterProcessor(src, regex));
    }


    /**
     * Add an regex filtering transformer to process chain that will exclude
     * matching items.
     *
     * @param src argument number
     *
     * @param regex regular expression
     *
     * @return augmented spy definition
     */
    public SpyDefinition filterOut(int src, String regex) {
        return withProcessor(new RegexFilterProcessor(src, regex, true));
    }


    public SpyDefinition transform(int src, int dst, String regex, String expr) {
        return transform(src, dst, regex, expr,  false);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param src
     * @param dst
     * @param regex
     * @param expr
     * @return
     */
    public SpyDefinition transform(int src, int dst, String regex, String expr, boolean filterOut) {
        return withProcessor(new RegexFilterProcessor(src, dst, regex, expr, filterOut));
    }


    /**
     * Normalizes a query string from src and puts result into dst.
     *
     * @param src
     * @param dst
     * @param normalizer
     * @return
     */
    public SpyDefinition normalize(int src, int dst, Normalizer normalizer) {
        return withProcessor(new NormalizingProcessor(src, dst, normalizer));
    }

    /**
     * Gets slot number n, performs traditional get operation and stores
     * results in the same slot.
     *
     * @param src
     *
     * @param dst
     *
     * @param path
     *
     * @return augmented spy definition
     */
    public SpyDefinition get(int src, int dst, Object...path) {
        return withProcessor(new GetterProcessor(src, dst, path));
    }


    /**
     *
     * @param slot
     * @param threadLocal
     * @return
     */
    public SpyDefinition get(int slot, ThreadLocal<Object> threadLocal, Object...path) {
        return withProcessor(new ThreadLocalProcessor(slot, ThreadLocalProcessor.GET, threadLocal, path));
    }


    /**
     *
     * @param slot
     * @param val
     * @return
     */
    public SpyDefinition put(int slot, Object val) {
        return withProcessor(new ConstPutProcessor(slot, val));
    }


    /**
     *
     * @param slot
     * @param threadLocal
     * @return
     */
    public SpyDefinition set(int slot, ThreadLocal<Object> threadLocal) {
        return withProcessor(new ThreadLocalProcessor(slot, ThreadLocalProcessor.SET, threadLocal));
    }


    /**
     *
     * @param threadLocal
     * @return
     */
    public SpyDefinition remove(ThreadLocal<Object> threadLocal) {
        return withProcessor(new ThreadLocalProcessor(0, ThreadLocalProcessor.REMOVE, threadLocal));
    }

    /**
     * Calculates time difference between in1 and in2 and stores result in out.
     *
     * @param tstart slot with tstart
     *
     * @param tstop slot with tstop
     *
     * @param dst destination slot
     *
     * @return augmented spy definition
     */
    public SpyDefinition timeDiff(int tstart, int tstop, int dst) {
        return withProcessor(new TimeDiffProcessor(tstart, tstop, dst));
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
    public SpyDefinition callMethod(int src, int dst, String methodName, Object... methodArgs) {
        return withProcessor(new MethodCallingProcessor(src, dst, methodName, methodArgs));
    }


    /**
     * Passes only records where (a op b) is true.
     *
     * @param a
     *
     * @param op
     *
     * @param b
     *
     * @return
     */
    public SpyDefinition ifSlotCmp(int a, int op, int b) {
        return withProcessor(ComparatorProcessor.scmp(a, op, b));
    }


    /**
     * Passes only records where (a op v) is true.
     *
     * @param a
     *
     * @param op
     *
     * @param v
     *
     * @return
     */
    public SpyDefinition ifValueCmp(int a, int op, Object v) {
        return withProcessor(ComparatorProcessor.vcmp(a, op, v));
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
        List<SpyCollector> lst = new ArrayList<SpyCollector>(collectors.size()+2);
        lst.addAll(collectors); lst.add(collector);
        sdef.collectors = lst;
        return sdef;
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
    public SpyDefinition toStats(String mbsName, String beanName, String attrName, String keyExpr,
                                 int tstampField, int timeField) {
        return toCollector(new ZorkaStatsCollector(mbsName, beanName, attrName, keyExpr, tstampField, timeField));
    }


    /**
     * Instructs spy to submit data to a single object of ZorkaStat object.
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @return augmented spy definition
     */
    public SpyDefinition toStat(String mbsName, String beanName, String attrName,
                                int tstampField, int timeField) {
        return toCollector(new JmxAttrCollector(mbsName, beanName, attrName, tstampField, timeField));
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
    public SpyDefinition toObjCall(This ns, String func) {
        return toCollector(new CallingObjCollector(ns, func));
    }


    /**
     * Instructs spy to submit data to a collect() function in a BSH namespace..
     *
     * @param ns BSH namespace
     *
     * @return augmented spy definition
     */
    public SpyDefinition toBsh(String ns) {
        return toCollector(new CallingBshCollector(ns));
    }


    /**
     * Instruct spy to submit data to log file.
     *
     * @param trapper
     *
     * @param logLevel
     *
     * @param expr
     *
     * @return
     */
    public SpyDefinition toFile(FileTrapper trapper, String logLevel, String expr) {
        return toCollector(new FileCollector(trapper, expr, ZorkaLogLevel.valueOf(logLevel), ""));
    }


    /**
     * Instructs spy to present attribute as an getter object.
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
    public SpyDefinition toGetter(String mbsName, String beanName, String attrName, String desc, int src, Object...path) {
        return toCollector(new GetterPresentingCollector(mbsName, beanName, attrName, desc, src, path));
    }


    /**
     * Sends collected records to another SpyDefinition chain. Records will be processed by all
     * processors from ON_COLLECT chain and sent to collectors attached to it.
     *
     * @param sdef
     *
     * @return
     */
    public SpyDefinition toSdef(SpyDefinition sdef) {
        return toCollector(new DispatchingCollector(sdef));
    }


    /**
     * Sends collected records as SNMP traps.
     *
     * @param trapper snmp trapper used to send traps;
     *
     * @param oid base OID - used as both enterprise OID in produced traps and prefix for variable OIDs;
     *
     * @param spcode - specific trap code; generic trap type is always enterpriseSpecific (6);
     *
     * @param bindings bindings defining additional variables attached to this trap;
     *
     * @return augmented spy definition
     */
    public SpyDefinition toSnmp(SnmpTrapper trapper, String oid, int spcode, TrapVarBindDef...bindings) {
        return toCollector(new SnmpCollector(trapper, oid, SnmpLib.GT_SPECIFIC, spcode, oid, bindings));
    }


    /**
     * Instruct spy to send collected record to syslog.
     *
     * @param trapper logger (object returned by syslog.get())
     *
     * @param expr message template
     *
     * @param severity
     *
     * @param facility
     *
     * @param hostname
     *
     * @param tag
     *
     * @return
     */
    public SpyDefinition toSyslog(SyslogTrapper trapper, String expr, int severity, int facility, String hostname, String tag) {
        return toCollector(new SyslogCollector(trapper, expr, severity, facility, hostname, tag));
    }


    /**
     * Sends collected records to zabbix using zabbix trapper.
     *
     * @param trapper
     *
     * @param expr
     *
     * @param key
     *
     * @return
     */
    public SpyDefinition toZabbix(ZabbixTrapper trapper, String expr, String key) {
        return toCollector(new ZabbixCollector(trapper, expr, null, key));
    }


    /**
     * Sends collected records to zabbix using zabbix trapper
     *
     * @param trapper
     *
     * @param expr
     *
     * @param host
     *
     * @param key
     *
     * @return
     */
    public SpyDefinition toZabbix(ZabbixTrapper trapper, String expr, String host, String key) {
        return toCollector(new ZabbixCollector(trapper, expr, host, key));
    }


    // TODO toString() method

    // TODO some method printing 'execution plan' of this SpyDef
}
