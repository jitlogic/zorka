/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.integ.ZorkaTrapper;
import com.jitlogic.zorka.integ.FileTrapper;
import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.snmp.SnmpTrapper;
import com.jitlogic.zorka.integ.snmp.TrapVarBindDef;
import com.jitlogic.zorka.integ.syslog.SyslogTrapper;
import com.jitlogic.zorka.integ.zabbix.ZabbixTrapper;
import com.jitlogic.zorka.normproc.Normalizer;
import com.jitlogic.zorka.spy.collectors.*;
import com.jitlogic.zorka.spy.probes.*;
import com.jitlogic.zorka.spy.processors.*;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLogLevel;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Spy library contains functions for configuring instrumentation engine. Spy definitions, matchers, probes, processors
 * and collectors can be created using functions from this library. Spy library is registered as 'spy' namespace in BSH.
 */
public class SpyLib {

    public static final String SM_NOARGS      = "<no-args>";
    public static final String SM_CONSTRUCTOR = "<init>";
    public static final String SM_ANY_TYPE    = null;
    public static final String SM_STATIC      = "<clinit>";


    public static final int SF_NONE = 0;
    public static final int SF_IMMEDIATE = 1;
    public static final int SF_FLUSH = 2;


    // Debug levels

    /** Be quiet */
    public static final int SPD_NONE = 0;

    /** Basic status messages */
    public static final int SPD_STATUS = 1;

    /** Detailed configuration information */
    public static final int SPD_CONFIG = 2;

    /** Log transformed classes */
    public static final int SPD_CLASSXFORM = 3;

    /** Log transformed methods */
    public static final int SPD_METHODXFORM = 4;

    /** Log all collected records reaching collector dispatcher */
    public static final int SPD_CDISPATCHES = 5;

    /** Log all collected records on each collector */
    public static final int SPD_COLLECTORS = 6;

    /** Log all argument processing events */
    public static final int SPD_ARGPROC = 7;

    /** Log all submissions from instrumented code */
    public static final int SPD_SUBMISSIONS = 8;

    /** Log all encountered methods (only from transformed classes) */
    public static final int SPD_METHODALL = 9;

    /** Log all classes going through transformer */
    public static final int SPD_CLASSALL = 10;

    /** Maximum possible debug log level */
    public static final int SPD_MAX = 10;


    public static final int GT = 0;
    public static final int GE = 1;
    public static final int EQ = 2;
    public static final int LE = 3;
    public static final int LT = 4;
    public static final int NE = 5;


    public static final int ON_ENTER   = 0;
    public static final int ON_RETURN  = 1;
    public static final int ON_ERROR   = 2;
    public static final int ON_SUBMIT  = 3;
    public static final int ON_COLLECT = 4;

    public static final int AC_PUBLIC       = 0x0001;
    public static final int AC_PRIVATE      = 0x0002;
    public static final int AC_PROTECTED    = 0x0004;
    public static final int AC_STATIC       = 0x0008;
    public static final int AC_FINAL        = 0x0010;
    public static final int AC_SUPER        = 0x0020;
    public static final int AC_SYNCHRONIZED = 0x0020;
    public static final int AC_VOLATILE     = 0x0040;
    public static final int AC_BRIDGE       = 0x0040;
    public static final int AC_VARARGS      = 0x0080;
    public static final int AC_TRANSIENT    = 0x0080;
    public static final int AC_NATIVE       = 0x0100;
    public static final int AC_INTERFACE    = 0x0200;
    public static final int AC_ABSTRACT     = 0x0400;
    public static final int AC_STRICT       = 0x0800;
    public static final int AC_SYNTHETIC    = 0x1000;
    public static final int AC_ANNOTATION   = 0x2000;
    public static final int AC_ENUM         = 0x4000;

    private SpyInstance instance;


	public SpyLib(SpyInstance instance) {
        this.instance = instance;
	}


    /**
     * Registers spy definition(s) in Zorka Spy instrumentation engine. Only definitions registered using this function
     * will be considered by class transformer when loading classes and thus can be instrumented.
     *
     * @param sdefs one or more spy definitions (created using spy.instance() or spy.instrument())
     */
    public void add(SpyDefinition...sdefs) {
        for (SpyDefinition sdef : sdefs) {
            instance.add(sdef);
        }
    }


    /**
     * Created an empty (unconfigured) spy definition. Use created object's methods to configure it before registering
     * with add() function.
     *
     * @return new spy definition
     */
    public SpyDefinition instance() {
        return SpyDefinition.instance();
    }


    public SpyDefinition instrument() {
        return SpyDefinition.instrument().onSubmit(tdiff("T1", "T2", "T")).onEnter();
    }


    /**
     * This is convenience function for monitoring execution times of methods. Execution times are stored in ZorkaStats
     * structure that is capable of storing statistics for multiple methods. Using expr argument it is possible to
     * use arbitrary criteria for categorizing methods (eg. by method name, by URL etc.)
     *
     * @param mbsName mbean server new bean will be registered in (typically 'java');
     *
     * @param mbeanName mbean name (existing one will be picked up if it is Zorka mapped MBean);
     *
     * @param attrName attribute name new stats object will be registered under;
     *
     * @param expr expression template that will be used as key for categorizing methods;
     *
     * @return new spy definition object;
     *
     */
    public SpyDefinition instrument(String mbsName, String mbeanName, String attrName, String expr) {

        Matcher m = ObjectInspector.reVarSubstPattern.matcher(expr);

        // Patch expression string to match argList data
        StringBuffer sb = new StringBuffer(expr.length()+4);
        List<SpyDefArg> sdaList = new ArrayList<SpyDefArg>();
        Set<String> usedArgs = new HashSet<String>();

        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                m.appendReplacement(sb, "\\${A" + ZorkaUtil.join(".", segs) + "}");
                if (!usedArgs.contains(segs[0])) {
                    usedArgs.add(segs[0]);
                    sdaList.add(fetchArg(Integer.parseInt(segs[0]), "A"+segs[0]));
                }
            }
        }

        m.appendTail(sb);

        sdaList.add(fetchTime("T1"));

        return SpyDefinition.instance()
                .onEnter(sdaList.toArray(new SpyDefArg[0]))
                .onReturn(fetchTime("T2")).onError(fetchTime("T2"))
                .onSubmit(tdiff("T1", "T2", "T"))
                .onCollect(zorkaStats(mbsName, mbeanName, attrName, sb.toString(), "T2", "T"));
    }


    /**
     * Creates new matcher object that will match classes by annotation.
     *
     * @param annotationName
     *
     * @return
     */
    public SpyMatcher byAnnotation(String annotationName) {
        return new SpyMatcher(SpyMatcher.CLASS_ANNOTATION, 1,
                "L" + annotationName + ";", "~[a-zA-Z_].*$", null);
    }

    /**
     * Creates new matcher object that will match methods by class annotation and method name.
     *
     * @param annotationName
     *
     * @param methodPattern
     *
     * @return
     */
    public SpyMatcher byAnnotation(String annotationName, String methodPattern) {
        return new SpyMatcher(SpyMatcher.CLASS_ANNOTATION, 1,
                "L" + annotationName + ";", methodPattern, null);
    }


    /**
     * Creates new matcher object that will match methods by class name and method name.
     *
     * @param classPattern class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                     regular expression (if starts with '~' character);
     *
     * @param methodPattern method name mask (where '*' means arbitrary name part) or regular expression
     *                      (if starts with '~' character);
     *
     * @return new matcher object
     */
    public SpyMatcher byMethod(String classPattern, String methodPattern) {
        return new SpyMatcher(0, 1, classPattern, methodPattern, null);
    }


    /**
     * Creates new matcher object that will match methods by class name, method name, access flags, return type and arguments.
     *
     * @param access access flags (use spy.ACC_* constants);
     *
     * @param classPattern class name mask (where * matches arbitrary name and ** matches arbitrary path) or
     *                     regular expression (if starts with '~' character);
     *
     * @param methodPattern method name mask (where '*' means arbitrary string) or regular expression (if starts with '~' char);
     *
     * @param retType return type (eg. void, int, String, javax.servlet.HttpResponse etc.);
     *
     * @param argTypes types of consecutive arguments;
     *
     * @return new matcher object;
     *
     */
    public SpyMatcher byMethod(int access, String classPattern, String methodPattern, String retType, String... argTypes) {
        return new SpyMatcher(0, access, classPattern,  methodPattern, retType, argTypes);
    }



    /**
     * Creates argument fetching probe. When injected into method code by instrumentation engine, it will fetch argument
     * selected by specific index `arg`.
     *
     * @param arg fetched argument index
     *
     * @param dst name (key) used to store fetched data
     *
     * @return new probe
     */
    public SpyProbe fetchArg(int arg, String dst) {
        return new SpyArgProbe(arg, dst);
    }


    /**
     * Creates class fetching probe. When injected into method code it will fetch class object of given name in context
     * of method caller.
     *
     * @param className class name
     *
     * @param dst name (key) used to store fetched data
     *
     * @return class fetching probe
     */
    public SpyProbe fetchClass(String className, String dst) {
        return new SpyClassProbe(className, dst);
    }


    /**
     * Creates exception fetching probe. When injected into method code it will fetch exception object when exception is
     * thrown out of method code.
     *
     * @param dst name (key) used to store fetched data
     *
     * @return exception fetching probe
     */
    public SpyProbe fetchException(String dst) {
        return new SpyReturnProbe(dst);
    }


    /**
     * Creates constant fetching probe. The actual probe injects null into method code but it is replaced by constant
     * value passed as first argument.
     *
     * @param val value to be added to records
     *
     * @param dst name (key) used to store fetched data
     *
     * @return constant fetching probe
     */
    public SpyProbe fetchConst(Object val, String dst) {
        return new SpyConstProbe(val, dst);
    }


    /**
     * Creates return value fetching probe. When injected into method code it will fetch return value of instrumented
     * method.
     *
     * @param dst name (key) used to store fetched data
     *
     * @return return value fetching probe
     */
    public SpyProbe fetchRetVal(String dst) {
        return new SpyReturnProbe(dst);
    }


    /**
     * Creates thread fetching probe. When injected into method code it will fetch current thread object.
     *
     * @param dst name (key) used to store fetched data
     *
     * @return thread fetching probe
     */
    public SpyProbe fetchThread(String dst) {
        return new SpyThreadProbe(dst);
    }


    /**
     * Creates time fetching probe. When injected into method code it will fetch current time.
     *
     * @param dst name (key) used to store fetched data
     *
     * @return time fetching probe
     */
    public SpyProbe fetchTime(String dst) {
        return new SpyTimeProbe(dst);
    }


    /**
     * Creates method call statistics collector object. It will maintain zorka call statistics and update them with
     * incoming data.
     *
     * @param mbsName mbean server name
     *
     * @param beanName bean name
     *
     * @param attrName attribute name
     *
     * @param keyExpr key expression
     *
     * @return collector object
     */
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr,
                                   String tstampField, String timeField) {
        return new ZorkaStatsCollector(mbsName, beanName, attrName, keyExpr, tstampField, timeField);
    }


    /**
     * Creates single method call statistics object. It will maintain zorka call statistics and update them with incoming
     * data.
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @return collector object
     */
    public SpyProcessor zorkaStat(String mbsName, String beanName, String attrName, String tstampField, String timeField) {
        return new JmxAttrCollector(mbsName, beanName, attrName, tstampField, timeField);
    }


    /**
     * Creates file colllector object. It will store collected records as text messages in log file.
     *
     * @param trapper file trapper to submit data to
     *
     * @param logLevel default log level
     *
     * @param expr message template expression
     *
     * @return collector object
     */
    public SpyProcessor fileCollector(FileTrapper trapper, String expr, ZorkaLogLevel logLevel) {
        return new FileCollector(trapper, expr, logLevel, "");
    }


    /**
     * Creates getter collector object. It will present collected records as attributes via mbeans.
     *
     * @param mbsName mbean server name
     *
     * @param beanName mbean name
     *
     * @param attrName attribute name
     *
     * @param path which stat attr to present
     *
     * @return collector object
     */
    public SpyProcessor getterCollector(String mbsName, String beanName, String attrName, String desc, String src, Object...path) {
        return new GetterPresentingCollector(mbsName, beanName, attrName, desc, src, path);
    }


    /**
     * Creates chained collector. It sends collected records to another SpyDefinition chain. Records will be processed
     * by all processors from ON_SUBMIT chain and sent to collectors attached to it.
     *
     * @param sdef new sdef that will perform furhter protessing
     *
     * @return chained collector
     */
    public SpyProcessor sdefCollector(SpyDefinition sdef) {
        return new DispatchingCollector(sdef);
    }


    /**
     * Creates SNMP collector object. It sends collected records as SNMP traps using SNMP trapper.
     *
     * @param trapper snmp trapper used to send traps;
     *
     * @param oid base OID - used as both enterprise OID in produced traps and prefix for variable OIDs;
     *
     * @param spcode - specific trap code; generic trap type is always enterpriseSpecific (6);
     *
     * @param bindings bindings defining additional variables attached to this trap;
     *
     * @return SNMP collector object
     */
    public SpyProcessor snmpCollector(SnmpTrapper trapper, String oid, int spcode, TrapVarBindDef...bindings) {
        return new SnmpCollector(trapper, oid, SnmpLib.GT_SPECIFIC, spcode, oid, bindings);
    }


    /**
     * Creates syslog collector object. It sends collected records to remote syslog server using syslog trapper.
     *
     * @param trapper trapper object used to send logs
     *
     * @param expr message template
     *
     * @param severity syslog serverity (see syslog.* constants)
     *
     * @param facility syslog facility
     *
     * @param hostname hostname (as logged in syslog records)
     *
     * @param tag syslog tag (typically program name, in our case component name)
     *
     * @return syslog collector object
     */
    public SpyProcessor syslogCollector(SyslogTrapper trapper, String expr, int severity, int facility, String hostname, String tag) {
        return new SyslogCollector(trapper, expr, severity, facility, hostname, tag);
    }


    /**
     * Sends collected records to specific trapper.
     *
     * @param trapper trapper object (eg. zabbix trapper, syslog trapper, file trapper etc.)
     *
     * @param tagExpr tag template (eg. class name or component name)
     *
     * @param msgExpr message template (used if no exception has been caught)
     *
     * @param errExpr error message template (if an exception has been caught)
     *
     * @param errField error field name
     *
     * @return trapper collector object
     *
     */
    public SpyProcessor trapperCollector(ZorkaTrapper trapper, String tagExpr, String msgExpr, String errExpr, String errField) {
        return new TrapperCollector(trapper, tagExpr, msgExpr,  errExpr,  errField);
    }


    /**
     * Sends collected records to zabbix using zabbix trapper.
     *
     * @param trapper zabbix trapper object (as created by zabbix.trapper() function)
     *
     * @param expr message template
     *
     * @param key zabbix key ID
     *
     * @return zabbix collector object
     */
    public SpyProcessor zabbixCollector(ZabbixTrapper trapper, String expr, String key) {
        return new ZabbixCollector(trapper, expr, null, key);
    }


    /**
     * Sends collected records to zabbix using zabbix trapper.
     *
     * @param trapper zabbix trapper (as created by zabbix.trapper() function)
     *
     * @param expr message template
     *
     * @param key zabbix key ID
     *
     * @return zabbix collector object
     */
    public SpyProcessor zabbixCollector(ZabbixTrapper trapper, String expr, String host, String key) {
        return new ZabbixCollector(trapper,  expr,  host,  key);
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
     * @return formatting processor object
     */
    public SpyProcessor format(String dst, String expr) {
        return new StringFormatProcessor(dst, expr);
    }


    /**
     * Filters records according to given regular expression.
     *
     * @param src argument number
     *
     * @param regex regular expression
     *
     * @return filtering processor object
     */
    public SpyProcessor regexFilter(String src, String regex) {
        return new RegexFilterProcessor(src, regex);
    }


    /**
     * Filters record according to given regular expression.
     *
     * @param src argument number
     *
     * @param regex regular expression
     *
     * @param filterOut inversed filtering if true
     *
     * @return filtering processor object
     */
    public SpyProcessor regexFilter(String src, String regex, boolean filterOut) {
        return new RegexFilterProcessor(src, regex, filterOut);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param src source field name
     *
     * @param dst destination field name
     *
     * @param regex regular expression used to parse input
     *
     * @param expr output value template (possibly using substrings taken from regex)
     *
     * @return transforming processor object
     */
    public SpyProcessor transform(String src, String dst, String regex, String expr) {
        return transform(src, dst, regex, expr, false);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param src source field name
     *
     * @param dst destination field name
     *
     * @param regex regular expression used to parse input
     *
     * @param expr output value template (possibly using substrings taken from regex)
     *
     * @param filterOut inverse regex treatment if true
     *
     * @return transforming processor object
     */
    public SpyProcessor transform(String src, String dst, String regex, String expr, boolean filterOut) {
        return new RegexFilterProcessor(src, dst, regex, expr, filterOut);
    }


    /**
     * Normalizes a query string from src and puts result into dst.
     *
     * @param src source field
     *
     * @param dst destination field
     *
     * @param normalizer normalizer object
     *
     * @return normalizing processor object
     */
    public SpyProcessor normalize(String src, String dst, Normalizer normalizer) {
        return new NormalizingProcessor(src, dst, normalizer);
    }


    /**
     * Gets slot number n, performs traditional get operation and stores
     * results in the same slot.
     *
     * @param src source field
     *
     * @param dst destination field
     *
     * @param path getter path
     *
     * @return getter processor object
     */
    public SpyProcessor get(String src, String dst, Object...path) {
        return new GetterProcessor(src, dst, path);
    }


    /**
     * Creates thread local getter processor. It gets a value from thread local object and stores it into dst field.
     *
     * @param dst destination field
     *
     * @param threadLocal source thread local object
     *
     * @return thread local getter processor object
     */
    public SpyProcessor tlGet(String dst, ThreadLocal<Object> threadLocal, Object...path) {
        return new ThreadLocalProcessor(dst, ThreadLocalProcessor.GET, threadLocal, path);
    }


    /**
     * Puts constant value into record.
     *
     * @param dst destination field
     *
     * @param val value
     *
     * @return constant value processor object
     */
    public SpyProcessor put(String dst, Object val) {
        return new ConstPutProcessor(dst, val);
    }


    /**
     * Creates thread local setter processor. It will fetch a field from record and store it in thread local.
     *
     * @param src source field
     *
     * @param threadLocal destination thread local object
     *
     * @return thread local setter object
     */
    public SpyProcessor tlSet(String src, ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(src, ThreadLocalProcessor.SET, threadLocal);
    }


    /**
     * Clears thread local object
     *
     * @param threadLocal thread local object to be cleaned up
     *
     * @return thread local cleaner object
     */
    public SpyProcessor tlRemove(ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(null, ThreadLocalProcessor.REMOVE, threadLocal);
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
     * @return time diff calculator object
     */
    public SpyProcessor tdiff(String tstart, String tstop, String dst) {
        return new TimeDiffProcessor(tstart, tstop, dst);
    }


    /**
     * Gets object from slot number arg, calls given method on this slot and
     * if method returns some value, stores its result in this slot.
     *
     * @param src source field
     *
     * @param dst destination field
     *
     * @param methodName method name
     *
     * @param methodArgs method arguments
     *
     * @return method calling processor object
     */
    public SpyProcessor call(String src, String dst, String methodName, Object... methodArgs) {
        return new MethodCallingProcessor(src, dst, methodName, methodArgs);
    }


    /**
     * Passes only records where (a op b) is true.
     *
     * @param a field name
     *
     * @param op operator
     *
     * @param b field name
     *
     * @return conditional filtering processor object
     */
    public SpyProcessor ifSlotCmp(String a, int op, String b) {
        return ComparatorProcessor.scmp(a, op, b);
    }


    /**
     * Passes only records where (a op v) is true.
     *
     * @param a field name
     *
     * @param op operator
     *
     * @param v reference value
     *
     * @return conditional filtering processor object
     */
    public SpyProcessor ifValueCmp(String a, int op, Object v) {
        return ComparatorProcessor.vcmp(a, op, v);
    }

}
