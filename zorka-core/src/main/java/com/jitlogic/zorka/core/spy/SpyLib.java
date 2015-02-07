/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.spy;

import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.*;
import com.jitlogic.zorka.core.integ.SnmpLib;
import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.integ.SnmpTrapper;
import com.jitlogic.zorka.core.integ.TrapVarBindDef;
import com.jitlogic.zorka.core.normproc.Normalizer;
import com.jitlogic.zorka.core.spy.plugins.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * Spy library contains functions for configuring instrumentation engine. Spy definitions, matchers, probes, processors
 * and collectors can be created using functions from this library. Spy library is registered as 'spy' namespace in BSH.
 */
public class SpyLib {

    private static final ZorkaLog log = ZorkaLogger.getLog(SpyLib.class);

    public static final String SM_NOARGS = "<no-args>";
    public static final String SM_CONSTRUCTOR = "<init>";
    public static final String SM_ANY_TYPE = null;
    public static final String SM_STATIC = "<clinit>";


    public static final int SF_NONE = 0;
    public static final int SF_IMMEDIATE = 1;
    public static final int SF_FLUSH = 2;

    public static final int ZST_STATS = 0x01;
    public static final int ZST_ENTER = 0x02;
    public static final int ZST_EXIT = 0x04;

    // Debug levels

    /**
     * Be quiet
     */
    public static final int SPD_NONE = 0;

    /**
     * Basic status messages
     */
    public static final int SPD_STATUS = 1;

    /**
     * Detailed configuration information
     */
    public static final int SPD_CONFIG = 2;

    /**
     * Log transformed classes
     */
    public static final int SPD_CLASSXFORM = 3;

    /**
     * Log transformed methods
     */
    public static final int SPD_METHODXFORM = 4;

    /**
     * Log all collected records reaching collector dispatcher
     */
    public static final int SPD_CDISPATCHES = 5;

    /**
     * Log all collected records on each collector
     */
    public static final int SPD_COLLECTORS = 6;

    /**
     * Log all argument processing events
     */
    public static final int SPD_ARGPROC = 7;

    /**
     * Log all submissions from instrumented code
     */
    public static final int SPD_SUBMISSIONS = 8;

    /**
     * Tracer debug messages
     */
    public static final int SPD_TRACE_DEBUG = 9;

    /**
     * All possible tracer messages
     */
    public static final int SPD_TRACE_ALL = 10;

    /**
     * Log all encountered methods (only from transformed classes)
     */
    public static final int SPD_METHODALL = 11;

    /**
     * Log all classes going through transformer
     */
    public static final int SPD_CLASSALL = 12;

    /**
     * Maximum possible debug log level
     */
    public static final int SPD_MAX = 13;


    public static final String GT = ">";
    public static final String GE = ">=";
    public static final String EQ = "==";
    public static final String LE = "<=";
    public static final String LT = "<";
    public static final String NE = "!=";

    public static final int ON_ENTER = 0;
    public static final int ON_RETURN = 1;
    public static final int ON_ERROR = 2;
    public static final int ON_SUBMIT = 3;

    public static final int AC_PUBLIC = 0x000001;
    public static final int AC_PRIVATE = 0x000002;
    public static final int AC_PROTECTED = 0x000004;
    public static final int AC_STATIC = 0x000008;
    public static final int AC_FINAL = 0x000010;
    public static final int AC_SUPER = 0x000020;
    public static final int AC_SYNCHRONIZED = 0x000020;
    public static final int AC_VOLATILE = 0x000040;
    public static final int AC_BRIDGE = 0x000040;
    public static final int AC_VARARGS = 0x000080;
    public static final int AC_TRANSIENT = 0x000080;
    public static final int AC_NATIVE = 0x000100;
    public static final int AC_INTERFACE = 0x000200;
    public static final int AC_ABSTRACT = 0x000400;
    public static final int AC_STRICT = 0x000800;
    public static final int AC_SYNTHETIC = 0x001000;
    public static final int AC_ANNOTATION = 0x002000;
    public static final int AC_ENUM = 0x004000;
    public static final int AC_PKGPRIV = 0x010000;
    public static final int AC_ANY = 0x000000;

    public static final int ACTION_STATS = 0x01;
    public static final int ACTION_ENTER = 0x02;
    public static final int ACTION_EXIT = 0x04;

    public static final String TRACE = "TRACE";
    public static final String DEBUG = "DEBUG";
    public static final String INFO = "INFO";
    public static final String WARN = "WARN";
    public static final String ERROR = "ERROR";
    public static final String FATAL = "FATAL";

    private SpyClassTransformer classTransformer;
    private MBeanServerRegistry mbsRegistry;

    /**
     * Creates spy library object
     *
     * @param classTransformer spy transformer
     */
    public SpyLib(SpyClassTransformer classTransformer, MBeanServerRegistry mbsRegistry) {
        this.classTransformer = classTransformer;
        this.mbsRegistry = mbsRegistry;
    }


    /**
     * Registers spy definition(s) in Zorka Spy instrumentation engine. Only definitions registered using this function
     * will be considered by class transformer when loading classes and thus can be instrumented.
     *
     * @param sdefs one or more spy definitions (created using spy.instance() or spy.instrument())
     */
    public void add(SpyDefinition... sdefs) {
        for (SpyDefinition sdef : sdefs) {
            classTransformer.add(sdef);
        }
    }

    private AtomicInteger anonymousSdef = new AtomicInteger(0);

    public SpyDefinition instance() {
        log.warn(ZorkaLogger.ZAG_CONFIG, "Attempt to create anonymous spy definition. "
                + "This API is depreciated as spy definitions should to be named since 0.9.12. "
                + "Sdef will be created for now BUT this will be forbidden in the future. " +
                "Error counter will be incremented as well, so administrator won't forget about this.");
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        return instance("anonymous-" + anonymousSdef.incrementAndGet());
    }


    /**
     * Created an empty (unconfigured) spy definition. Use created object's methods to configure it before registering
     * with add() function.
     *
     * @return new spy definition
     */
    public SpyDefinition instance(String name) {
        return SpyDefinition.instance(name);
    }


    public SpyDefinition instrument() {
        log.warn(ZorkaLogger.ZAG_CONFIG, "Attempt to create anonymous spy definition. "
                + "This API is depreciated as spy definitions should to be named since 0.9.12. "
                + "Sdef will be created for now BUT this will be forbidden in the future. " +
                "Error counter will be incremented as well, so administrator won't forget about this.");
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        return instrument("anonymous-" + anonymousSdef.incrementAndGet());
    }


    /**
     * Returns partially configured time-measuring spy def
     *
     * @return partially configured psy def
     */
    public SpyDefinition instrument(String name) {
        return SpyDefinition.instance(name)
                .onEnter(fetchTime("T1"))
                .onReturn(fetchTime("T2"))
                .onError(fetchTime("T2"))
                .onSubmit(tdiff("T", "T1", "T2"));
    }


    public SpyDefinition instrument(String mbsName, String mbeanName, String attrName, String expr) {
        log.warn(ZorkaLogger.ZAG_CONFIG, "Attempt to create anonymous spy definition. "
                + "This API is depreciated as spy definitions should to be named since 0.9.12. "
                + "Sdef will be created for now BUT this will be forbidden in the future. " +
                "Error counter will be incremented as well, so administrator won't forget about this.");
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
        return instrument("anonymous-" + anonymousSdef.incrementAndGet(), mbsName, mbeanName, attrName, expr);
    }


    /**
     * This is convenience function for monitoring execution times of methods. Execution times are stored in ZorkaStats
     * structure that is capable of storing statistics for multiple methods. Using expr argument it is possible to
     * use arbitrary criteria for categorizing methods (eg. by method name, by URL etc.)
     *
     * @param mbsName   mbean server new bean will be registered in (typically 'java');
     * @param mbeanName mbean name (existing one will be picked up if it is Zorka mapped MBean);
     * @param attrName  attribute name new stats object will be registered under;
     * @param expr      expression template that will be used as key for categorizing methods;
     * @return new spy definition object;
     */
    public SpyDefinition instrument(String name, String mbsName, String mbeanName, String attrName, String expr) {

        log.warn(ZorkaLogger.ZAG_CONFIG, "Function spy.instrument(mbsName, mbeanName, attrName, expr) is deprecated due to lack of utility. "
                + "Sdef will be created for now BUT this will be forbidden in the future. " +
                "Error counter will be incremented as well, so administrator won't forget about this.");
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);

        Matcher m = ObjectInspector.reVarSubstPattern.matcher(expr);

        // Patch expression string to match argList data
        StringBuffer sb = new StringBuffer(expr.length() + 4);
        List<SpyDefArg> sdaList = new ArrayList<SpyDefArg>();
        Set<String> usedArgs = new HashSet<String>();

        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                m.appendReplacement(sb, "\\${A" + ZorkaUtil.join(".", segs) + "}");
                if (!usedArgs.contains(segs[0])) {
                    usedArgs.add(segs[0]);
                    sdaList.add(fetchArg("A" + segs[0], Integer.parseInt(segs[0])));
                }
            }
        }

        m.appendTail(sb);

        sdaList.add(fetchTime("T1"));

        return SpyDefinition.instance(name)
                .onEnter(sdaList.toArray(new SpyDefArg[0]))
                .onReturn(fetchTime("T2")).onError(fetchTime("T2"))
                .onSubmit(tdiff("T", "T1", "T2"), zorkaStats(mbsName, mbeanName, attrName, sb.toString()));
    }


    /**
     * Creates new matcher object that will match classes by annotation.
     *
     * @param annotationName class annotation pattern
     * @return spy matcher object
     */
    public SpyMatcher byClassAnnotation(String annotationName) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_ANNOTATION, 1,
                "L" + annotationName + ";", "~[a-zA-Z_].*$", null);
    }

    /**
     * Creates new matcher object that will match methods by class annotation and method name.
     *
     * @param annotationName class annotation pattern
     * @param methodPattern  method name pattern
     * @return spy matcher object
     */
    public SpyMatcher byClassAnnotation(String annotationName, String methodPattern) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_ANNOTATION | SpyMatcher.BY_METHOD_NAME, 1,
                "L" + annotationName + ";", methodPattern, null);
    }


    /**
     * Creates new matcher that will match all public methods of given class.
     *
     * @param className class name (or mask)
     * @return spy matched object
     */
    public SpyMatcher byClass(String className) {
        return byMethod(className, "*");
    }

    /**
     * Creates new matcher that will match all public methods of given class.
     *
     * @param iClassName interface class name (or mask)
     * @return spy matched object
     */
    public SpyMatcher byInterface(String iClassName) {
        return byInterfaceAndMethod(iClassName, "*");
    }

    /**
     * Creates new matcher that will match methods by method annotation.
     *
     * @param classPattern     class name pattern
     * @param methodAnnotation method annotation patten
     * @return spy matcher object
     */
    public SpyMatcher byMethodAnnotation(String classPattern, String methodAnnotation) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_NAME | SpyMatcher.BY_METHOD_ANNOTATION, 1,
                classPattern, "L" + methodAnnotation + ";", null);
    }


    /**
     * Creates new matcher that will match methods by class and method annotations
     *
     * @param classAnnotation  class annotation pattern
     * @param methodAnnotation method annotation pattern
     * @return spy matcher object
     */
    public SpyMatcher byClassMethodAnnotation(String classAnnotation, String methodAnnotation) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_ANNOTATION | SpyMatcher.BY_METHOD_ANNOTATION, 1,
                "L" + classAnnotation + ";", "L" + methodAnnotation + ";", null);
    }


    /**
     * Creates new matcher object that will match methods by class name and method name.
     *
     * @param iClassPattern interface class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                      regular expression (if starts with '~' character);
     * @param methodPattern method name mask (where '*' means arbitrary name part) or regular expression
     *                      (if starts with '~' character);
     * @return new matcher object
     */
    public SpyMatcher byInterfaceAndMethod(String iClassPattern, String methodPattern) {
        return new SpyMatcher(SpyMatcher.BY_INTERFACE | SpyMatcher.BY_METHOD_NAME, 1, iClassPattern, methodPattern, null);
    }


    /**
     * Creates new matcher object that will match methods by class name, method name, access flags, return type and arguments.
     *
     * @param access        access flags (use spy.ACC_* constants);
     * @param iClassPattern interface class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                      regular expression (if starts with '~' character);
     * @param methodPattern method name mask (where '*' means arbitrary string) or regular expression (if starts with '~' char);
     * @param retType       return type (eg. void, int, String, javax.servlet.HttpResponse etc.);
     * @param argTypes      types of consecutive arguments;
     * @return new matcher object;
     */
    public SpyMatcher byInterfaceAndMethod(int access, String iClassPattern, String methodPattern, String retType, String... argTypes) {
        return new SpyMatcher(SpyMatcher.BY_INTERFACE | SpyMatcher.BY_METHOD_NAME | SpyMatcher.BY_METHOD_SIGNATURE,
                access, iClassPattern, methodPattern, retType, argTypes);
    }


    /**
     * Creates new matcher object that will match methods by class name and method name.
     *
     * @param classPattern  class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                      regular expression (if starts with '~' character);
     * @param methodPattern method name mask (where '*' means arbitrary name part) or regular expression
     *                      (if starts with '~' character);
     * @return new matcher object
     */
    public SpyMatcher byMethod(String classPattern, String methodPattern) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_NAME | SpyMatcher.BY_METHOD_NAME, 1, classPattern, methodPattern, null);
    }


    /**
     * Creates new matcher object that will match methods by class name, method name, access flags, return type and arguments.
     *
     * @param access        access flags (use spy.ACC_* constants);
     * @param classPattern  class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                      regular expression (if starts with '~' character);
     * @param methodPattern method name mask (where '*' means arbitrary string) or regular expression (if starts with '~' char);
     * @param retType       return type (eg. void, int, String, javax.servlet.HttpResponse etc.);
     * @param argTypes      types of consecutive arguments;
     * @return new matcher object;
     */
    public SpyMatcher byMethod(int access, String classPattern, String methodPattern, String retType, String... argTypes) {
        return new SpyMatcher(SpyMatcher.BY_CLASS_NAME | SpyMatcher.BY_METHOD_NAME | SpyMatcher.BY_METHOD_SIGNATURE,
                access, classPattern, methodPattern, retType, argTypes);
    }


    /**
     * Creates argument fetching probe. When injected into method code by instrumentation engine, it will fetch argument
     * selected by specific index `arg`.
     *
     * @param dst name (key) used to store fetched data
     * @param arg fetched argument index
     * @return new probe
     */
    public SpyProbe fetchArg(String dst, int arg) {
        return new SpyArgProbe(arg, dst);
    }


    /**
     * Creates class fetching probe. When injected into method code it will fetch class object of given name in context
     * of method caller.
     *
     * @param dst       name (key) used to store fetched data
     * @param className class name
     * @return class fetching probe
     */
    public SpyProbe fetchClass(String dst, String className) {
        return new SpyClassProbe(dst, className);
    }


    /**
     * Creates exception fetching probe. When injected into method code it will fetch exception object when exception is
     * thrown out of method code.
     *
     * @param dst name (key) used to store fetched data
     * @return exception fetching probe
     */
    public SpyProbe fetchError(String dst) {
        return new SpyReturnProbe(dst);
    }


    /**
     * Creates return value fetching probe. When injected into method code it will fetch return value of instrumented
     * method.
     *
     * @param dst name (key) used to store fetched data
     * @return return value fetching probe
     */
    public SpyProbe fetchRetVal(String dst) {
        return new SpyReturnProbe(dst);
    }


    /**
     * Creates thread fetching probe. When injected into method code it will fetch current thread object.
     *
     * @param dst name (key) used to store fetched data
     * @return thread fetching probe
     */
    public SpyProbe fetchThread(String dst) {
        return new SpyThreadProbe(dst);
    }


    /**
     * Creates time fetching probe. When injected into method code it will fetch current time.
     *
     * @param dst name (key) used to store fetched data
     * @return time fetching probe
     */
    public SpyProbe fetchTime(String dst) {
        return new SpyTimeProbe(dst);
    }


    /**
     * Marks current record as error.
     *
     * @return
     */
    public SpyProcessor markError() {
        return new SpyFlagsProcessor(true);
    }

    public void markError(Map<String,Object> record) {
        int f = (Integer)record.get(".STAGES");
        record.put(".STAGES", ((f | SpyLib.ON_ERROR) & ~SpyLib.ON_RETURN));
    }

    public void unmarkError(Map<String,Object> record) {
        int f = (Integer)record.get(".STAGES");
        record.put(".STAGES", ((f | SpyLib.ON_RETURN) & ~SpyLib.ON_ERROR));
    }

    /**
     * Creates method call statistics collector object. It will maintain zorka call statistics and update them with
     * incoming data.
     *
     * @param mbsName  mbean server name
     * @param beanName bean name
     * @param attrName attribute name
     * @param keyExpr  key expression
     * @return collector object
     */
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr) {
        return zorkaStats(mbsName, beanName, attrName, keyExpr, "T");
    }


    /**
     * Creates method call statistics collector object. It will maintain zorka call statistics and update them with
     * incoming data.
     *
     * @param mbsName   mbean server name
     * @param beanName  bean name
     * @param attrName  attribute name
     * @param keyExpr   key expression
     * @param timeField field containing execution time (in nanoseconds)
     * @return collector object
     */
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr, String timeField) {
        return new ZorkaStatsCollector(mbsRegistry, mbsName, beanName, attrName, keyExpr, timeField,
                null, ZorkaStatsCollector.ACTION_STATS);
    }


    /**
     * Creates method call statistics collector object. It will maintain zorka call statistics and update them with
     * incoming data. This variant also calculates throughput from supplied field.
     *
     * @param mbsName         mbean server name
     * @param beanName        bean name
     * @param attrName        attribute name
     * @param keyExpr         key expression
     * @param timeField       field containing execution time (in nanoseconds)
     * @param throughputField field containing throughput value (or null to skip throughput calculation)
     * @return collector object
     */
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr,
                                   String timeField, String throughputField) {
        return new ZorkaStatsCollector(mbsRegistry, mbsName, beanName, attrName, keyExpr, timeField,
                throughputField, ZorkaStatsCollector.ACTION_STATS);
    }


    /**
     * Creates method call statistics collector object. It will maintain zorka call statistics and update them with
     * incoming data. This variant also calculates throughput from supplied field.
     *
     * @param mbsName         mbean server name
     * @param beanName        bean name
     * @param attrName        attribute name
     * @param keyExpr         key expression
     * @param timeField       field containing execution time (in nanoseconds)
     * @param throughputField field containing throughput value (or null to skip throughput calculation)
     * @param actions         which actions will be performed: ENTER, EXIT or STATS (or combination of them)
     * @return collector object
     */
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr,
                                   String timeField, String throughputField, int actions) {
        return new ZorkaStatsCollector(mbsRegistry, mbsName, beanName, attrName, keyExpr, timeField, throughputField, actions);
    }


    /**
     * Creates getter collector object. It will present collected records as attributes via mbeans.
     *
     * @param mbsName  mbean server name
     * @param beanName mbean name
     * @param attrName attribute name
     * @param path     which stat attr to present
     * @return collector object
     */
    public SpyProcessor toGetter(String mbsName, String beanName, String attrName, String desc, String src, Object... path) {
        return new GetterPresentingCollector(mbsRegistry, mbsName, beanName, attrName, desc, src, path);
    }


    /**
     * Creates asynchronous queuing collector
     *
     * @param attrs attributes to retain
     * @return asynchronous queued collector
     */
    public SpyProcessor asyncCollector(String... attrs) {
        return new AsyncQueueCollector(attrs);
    }

    /**
     * Creates SNMP collector object. It sends collected records as SNMP traps using SNMP trapper.
     *
     * @param trapper  snmp trapper used to send traps;
     * @param oid      base OID - used as both enterprise OID in produced traps and prefix for variable OIDs;
     * @param spcode   - specific trap code; generic trap type is always enterpriseSpecific (6);
     * @param bindings bindings defining additional variables attached to this trap;
     * @return SNMP collector object
     */
    public SpyProcessor snmpCollector(SnmpTrapper trapper, String oid, int spcode, TrapVarBindDef... bindings) {
        return new SnmpCollector(trapper, oid, SnmpLib.GT_SPECIFIC, spcode, oid, bindings);
    }


    /**
     * Sends collected records to specific trapper.
     *
     * @param trapper trapper object (eg. zabbix trapper, syslog trapper, file trapper etc.)
     * @param tagExpr tag template (eg. class name or component name)
     * @param msgExpr message template (used if no exception has been caught)
     * @return trapper collector object
     */
    public SpyProcessor trapperCollector(ZorkaTrapper trapper, ZorkaLogLevel logLevel, String tagExpr, String msgExpr) {
        return trapper != null ? new TrapperCollector(trapper, logLevel, tagExpr, msgExpr, null, null) : null;
    }


    /**
     * Sends collected records to specific trapper.
     *
     * @param trapper  trapper object (eg. zabbix trapper, syslog trapper, file trapper etc.)
     * @param tagExpr  tag template (eg. class name or component name)
     * @param msgExpr  message template (used if no exception has been caught)
     * @param errExpr  error message template (if an exception has been caught)
     * @param errField error field name
     * @return trapper collector object
     */
    public SpyProcessor trapperCollector(ZorkaTrapper trapper, ZorkaLogLevel logLevel,
                                         String tagExpr, String msgExpr, String errExpr, String errField) {
        return new TrapperCollector(trapper, logLevel, tagExpr, msgExpr, errExpr, errField);
    }


    /**
     * Logs record occurence in zorka log. This is useful for debug purposes.
     *
     * @param logLevel log level
     * @param tag      tag
     * @param message  message template (will be filled with record fields if necessary)
     * @return logger collector object
     */
    public SpyProcessor zorkaLog(String logLevel, String tag, String message) {
        return new ZorkaLogCollector(ZorkaLogLevel.valueOf(logLevel), tag, message, null, null);
    }


    /**
     * Logs record occurence in zorka log. This is useful for debug purposes.
     *
     * @param logLevel log level
     * @param tag      tag
     * @param message  message template (will be filled with record fields if necessary)
     * @param fErr
     * @return logger collector object
     */
    public SpyProcessor zorkaLog(String logLevel, String tag, String message, String fErr) {
        return new ZorkaLogCollector(ZorkaLogLevel.valueOf(logLevel), tag, message, null, fErr);
    }


    /**
     * Logs record occurence in zorka log. This is useful for debug purposes.
     *
     * @param logLevel log level
     * @param tag      tag
     * @param message  message template (will be filled with record fields if necessary)
     * @param fCond    condition field - record will be logged if condition is not null and not equal to Boolean.FALSE
     * @return logger collector object
     */
    public SpyProcessor zorkaLogCond(String logLevel, String tag, String message, String fCond) {
        return new ZorkaLogCollector(ZorkaLogLevel.valueOf(logLevel), tag, message, fCond, null);
    }


    /**
     * Logs record occurence in zorka log. This is useful for debug purposes.
     *
     * @param logLevel log level
     * @param tag      tag
     * @param message  message template (return path, will be filled with record fields if necessary)
     * @param fCond    condition field - record will be logged if condition is not null and not equal to Boolean.FALSE
     * @param fErr     message tempalte (error path, will be filled with record field if necessary)
     * @return logger collector object
     */
    public SpyProcessor zorkaLogCond(String logLevel, String tag, String message, String fCond, String fErr) {
        return new ZorkaLogCollector(ZorkaLogLevel.valueOf(logLevel), tag, message, fCond, fErr);
    }

    /**
     * Formats arguments and passes an array of formatted strings.
     * Format expression is generally a string with special marker for
     * extracting previous arguments '${n.field1.field2...}' where n is argument
     * number, field1,field2,... are (optional) fields used exactly as in
     * zorkalib.get() function.
     *
     * @param dst  destination field
     * @param expr format expressions.
     * @param len  maximum result string length
     * @return formatting processor object
     */
    public SpyProcessor format(String dst, String expr, int len) {
        return new StringFormatProcessor(dst, expr, len);
    }

    public SpyProcessor format(String dst, String expr) {
        return format(dst, expr, -1);
    }

    /**
     * Filters records according to given regular expression.
     *
     * @param dst   destination slot
     * @param regex regular expression
     * @return filtering processor object
     */
    public SpyProcessor regexFilter(String dst, String regex) {
        return new RegexFilterProcessor(dst, regex);
    }


    /**
     * Filters record according to given regular expression.
     *
     * @param dst   destination slot
     * @param regex regular expression
     * @return filtering processor object
     */
    public SpyProcessor regexFilterOut(String dst, String regex) {
        return new RegexFilterProcessor(dst, regex, true);
    }


    public SpyProcessor valSetFilter(String field, Set<?> candidates) {
        return new SetFilterProcessor(field, false, candidates);
    }


    public SpyProcessor valSetFilterOut(String field, Set<?> candidates) {
        return new SetFilterProcessor(field, true, candidates);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param dst   destination field name
     * @param src   source field name
     * @param regex regular expression used to parse input
     * @param expr  output value template (possibly using substrings taken from regex)
     * @return transforming processor object
     */
    public SpyProcessor transform(String dst, String src, String regex, String expr) {
        return transform(dst, src, regex, expr, false);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param dst       destination field name
     * @param src       source field name
     * @param regex     regular expression used to parse input
     * @param expr      output value template (possibly using substrings taken from regex)
     * @param filterOut inverse regex treatment if true
     * @return transforming processor object
     */
    public SpyProcessor transform(String dst, String src, String regex, String expr, boolean filterOut) {
        return new RegexFilterProcessor(src, dst, regex, expr, filterOut);
    }


    /**
     * Normalizes a query string from src and puts result into dst.
     *
     * @param dst        destination field
     * @param src        source field
     * @param normalizer normalizer object
     * @return normalizing processor object
     */
    public SpyProcessor normalize(String dst, String src, Normalizer normalizer) {
        return new NormalizingProcessor(src, dst, normalizer);
    }


    /**
     * Gets slot number n, performs traditional get operation and stores
     * results in the same slot.
     *
     * @param dst  destination field
     * @param src  source field
     * @param path getter path
     * @return getter processor object
     */
    public SpyProcessor get(String dst, String src, Object... path) {
        return new GetterProcessor(src, dst, path);
    }


    /**
     * Creates thread local getter processor. It gets a value from thread local object and stores it into dst field.
     *
     * @param dst         destination field
     * @param threadLocal source thread local object
     * @return thread local getter processor object
     */
    public SpyProcessor tlGet(String dst, ThreadLocal<Object> threadLocal, Object... path) {
        return new ThreadLocalProcessor(dst, ThreadLocalProcessor.GET, threadLocal, path);
    }


    /**
     * Puts constant value into record.
     *
     * @param dst destination field
     * @param val value
     * @return constant value processor object
     */
    public SpyProcessor put(String dst, Object val) {
        return new ConstValProcessor(dst, val);
    }


    /**
     * Creates thread local setter processor. It will fetch a field from record and store it in thread local.
     *
     * @param src         source field
     * @param threadLocal destination thread local object
     * @return thread local setter object
     */
    public SpyProcessor tlSet(String src, ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(src, ThreadLocalProcessor.SET, threadLocal);
    }


    /**
     * Clears thread local object
     *
     * @param threadLocal thread local object to be cleaned up
     * @return thread local cleaner object
     */
    public SpyProcessor tlRemove(ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(null, ThreadLocalProcessor.REMOVE, threadLocal);
    }


    /**
     * Calculates time difference between in1 and in2 and stores result in out.
     *
     * @param dst    destination slot
     * @param tstart slot with tstart
     * @param tstop  slot with tstop
     * @return time diff calculator object
     */
    public SpyProcessor tdiff(String dst, String tstart, String tstop) {
        return new TimeDiffProcessor(tstart, tstop, dst);
    }


    /**
     * Gets object from slot number arg, calls given method on this slot and
     * if method returns some value, stores its result in this slot.
     *
     * @param dst        destination field
     * @param src        source field
     * @param methodName method name
     * @param methodArgs method arguments
     * @return method calling processor object
     */
    public SpyProcessor call(String dst, String src, String methodName, Object... methodArgs) {
        return new MethodCallingProcessor(src, dst, methodName, methodArgs);
    }


    /**
     * Passes only records where (a op b) is true.
     *
     * @param a  field name
     * @param op operator
     * @param b  field name
     * @return conditional filtering processor object
     */
    public SpyProcessor scmp(String a, String op, String b) {
        return ComparatorProcessor.scmp(a, op, b);
    }


    /**
     * Passes only records where (a op v) is true.
     *
     * @param a  field name
     * @param op operator
     * @param v  reference value
     * @return conditional filtering processor object
     */
    public SpyProcessor vcmp(String a, String op, Object v) {
        return ComparatorProcessor.vcmp(a, op, v);
    }


    public SpyProcessor stringMatcher(String srcField, List<String> includes, List<String> excludes) {
        return new StringMatcherProcessor(srcField, includes, excludes);
    }


    /**
     * Passes only records of method calls that took longer than specified interval.
     * Execution time is taken from default slot called "T".
     *
     * @param interval minimum execution interval (milliseconds)
     * @return conditional spy processor
     */
    public SpyProcessor longerThan(long interval) {
        return longerThan("T", interval);
    }


    /**
     * Passes only records of method calls that took longer than specified interval.
     *
     * @param dst      slot where execution time value is stored
     * @param interval minimum execution interval (milliseconds)
     * @return conditional spy processor
     */
    public SpyProcessor longerThan(String dst, long interval) {
        return ComparatorProcessor.vcmp(dst, ">", interval * 1000000L);
    }


    /**
     * Calculates CRC32 sum and stores it as hexified string
     *
     * @param dst destination field
     * @param src source field
     * @return processor object
     */
    public SpyProcessor crc32sum(String dst, String src) {
        return crc32sum(dst, src, CheckSumProcessor.MAX_LIMIT);
    }


    /**
     * Calculates CRC32 sum and stores it as hexified string
     *
     * @param dst   destination field
     * @param src   source field
     * @param limit maximum length of resulting string
     * @return processor object
     */
    public SpyProcessor crc32sum(String dst, String src, int limit) {
        return new CheckSumProcessor(dst, src, CheckSumProcessor.CRC32_TYPE, limit);
    }


    /**
     * Calculates MD5 sum and stores it as hexified string
     *
     * @param dst destination field
     * @param src source field
     * @return processor object
     */
    public SpyProcessor md5sum(String dst, String src) {
        return md5sum(dst, src, CheckSumProcessor.MAX_LIMIT);
    }


    /**
     * Calculates MD5 sum and stores it as hexified string
     *
     * @param dst   destination field
     * @param src   source field
     * @param limit maximum length of resulting string
     * @return processor object
     */
    public SpyProcessor md5sum(String dst, String src, int limit) {
        return new CheckSumProcessor(dst, src, CheckSumProcessor.MD5_TYPE, limit);
    }


    /**
     * Calculates SHA1 sum and stores it as hexified string
     *
     * @param dst destination field
     * @param src source field
     * @return processor object
     */
    public SpyProcessor sha1sum(String dst, String src) {
        return sha1sum(dst, src, CheckSumProcessor.MAX_LIMIT);
    }


    /**
     * Calculates SHA1 sum and stores it as hexified string
     *
     * @param dst   destination field
     * @param src   source field
     * @param limit maximum length of resulting string
     * @return processor object
     */
    public SpyProcessor sha1sum(String dst, String src, int limit) {
        return new CheckSumProcessor(dst, src, CheckSumProcessor.SHA1_TYPE, limit);
    }


    /**
     * Return time in human readable form. Time is taken from "T" field.
     *
     * @param dst destination field
     * @return strTime function spy processor
     */
    public SpyProcessor strTime(String dst) {
        return strTime(dst, "T");
    }


    /**
     * Return time in human readable form.
     *
     * @param dst destination field
     * @param src source field (must be Long, time in nanoseconds)
     * @return strTime function spy processor
     */
    public SpyProcessor strTime(String dst, String src) {
        return UtilFnProcessor.strTimeFn(dst, src);
    }

    public SpyProcessor strClock(String dst, String src) {
        return UtilFnProcessor.strClockFn(dst, src);
    }


    public SpyProcessor and(SpyProcessor... processors) {
        return new LogicalFilterProcessor(LogicalFilterProcessor.FILTER_AND, processors);
    }

    public SpyProcessor or(SpyProcessor... processors) {
        return new LogicalFilterProcessor(LogicalFilterProcessor.FILTER_OR, processors);
    }

    public SpyProcessor subchain(SpyProcessor... processors) {
        return new LogicalFilterProcessor(LogicalFilterProcessor.FILTER_NONE, processors);
    }
}
