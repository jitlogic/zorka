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

import bsh.This;
import com.jitlogic.zorka.agent.FileTrapper;
import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.snmp.SnmpTrapper;
import com.jitlogic.zorka.integ.snmp.TrapVarBindDef;
import com.jitlogic.zorka.integ.syslog.SyslogTrapper;
import com.jitlogic.zorka.integ.zabbix.ZabbixTrapper;
import com.jitlogic.zorka.logproc.LogProcessor;
import com.jitlogic.zorka.normproc.Normalizer;
import com.jitlogic.zorka.spy.collectors.*;
import com.jitlogic.zorka.spy.processors.*;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLogLevel;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;
import java.util.regex.Matcher;

/**
 * This is API for zorka users.
 */
public class SpyLib {

    public final static int GT = 0;
    public final static int GE = 1;
    public final static int EQ = 2;
    public final static int LE = 3;
    public final static int LT = 4;
    public final static int NE = 5;


    public static final int ON_ENTER   = 0;
    public static final int ON_RETURN  = 1;
    public static final int ON_ERROR   = 2;
    public static final int ON_SUBMIT  = 3;
    public static final int ON_COLLECT = 4;

    public static final int FETCH_TIME   = -1;
    public static final int FETCH_RETVAL = -2;
    public static final int FETCH_ERROR  = -3;
    public static final int FETCH_THREAD = -4;
    public static final int FETCH_CLASS  = -5;
    public static final int FETCH_NULL   = -6;

    public static int AC_PUBLIC       = 0x0001;
    public static int AC_PRIVATE      = 0x0002;
    public static int AC_PROTECTED    = 0x0004;
    public static int AC_STATIC       = 0x0008;
    public static int AC_FINAL        = 0x0010;
    public static int AC_SUPER        = 0x0020;
    public static int AC_SYNCHRONIZED = 0x0020;
    public static int AC_VOLATILE     = 0x0040;
    public static int AC_BRIDGE       = 0x0040;
    public static int AC_VARARGS      = 0x0080;
    public static int AC_TRANSIENT    = 0x0080;
    public static int AC_NATIVE       = 0x0100;
    public static int AC_INTERFACE    = 0x0200;
    public static int AC_ABSTRACT     = 0x0400;
    public static int AC_STRICT       = 0x0800;
    public static int AC_SYNTHETIC    = 0x1000;
    public static int AC_ANNOTATION   = 0x2000;
    public static int AC_ENUM         = 0x4000;

    private SpyInstance instance;


	public SpyLib(SpyInstance instance) {
        this.instance = instance;
	}


    public void add(SpyDefinition...sdefs) {
        for (SpyDefinition sdef : sdefs) {
            instance.add(sdef);
        }
    }


    public SpyDefinition instance() {
        return SpyDefinition.instance();
    }


    public SpyDefinition instrument() {
        return SpyDefinition.instrument().onSubmit(tdiff(0, 1, 1)).onEnter();
    }


    // TODO instrument(String expr) convenience function;
    public SpyDefinition instrument(String mbsName, String mbeanName, String attrName, String expr) {

        List<Integer> argList = new ArrayList<Integer>();

        Matcher m = ObjectInspector.reVarSubstPattern.matcher(expr);

        // Find out all used arguments
        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                Integer arg = Integer.parseInt(segs[0]);
                if (!argList.contains(arg)) {
                    argList.add(arg);
                }
            }
        }

        // Patch expression string to match argList data
        StringBuffer sb = new StringBuffer(expr.length()+4);
        m = ObjectInspector.reVarSubstPattern.matcher(expr);

        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                segs[0] = ""+argList.indexOf(Integer.parseInt(segs[0]));
                m.appendReplacement(sb, "\\${" + ZorkaUtil.join(".", segs) + "}");
            }
        }

        m.appendTail(sb);


        // Create and return spy definition

        int tidx = argList.size();

        List<SpyDefArg> sdaList = new ArrayList<SpyDefArg>(argList.size()+2);

        for (Integer arg : argList) {
            sdaList.add(fetchArg(arg));
        }

        sdaList.add(fetchTime());

        return SpyDefinition.instance()
                .onEnter(sdaList.toArray(new SpyDefArg[0]))
                .onReturn(fetchTime()).onError(fetchTime())
                .onSubmit(tdiff(tidx, tidx + 1, tidx + 1))
                .onCollect(zorkaStats(mbsName, mbeanName, attrName, sb.toString(), tidx, tidx + 1));
    }


    public SpyMatcher byMethod(String classPattern, String methodPattern) {
        return new SpyMatcher(1, classPattern, methodPattern, null);
    }


    public SpyMatcher byMethod(int access, String classPattern, String methodPattern, String retType, String... argTypes) {
        return new SpyMatcher(access, classPattern,  methodPattern, retType, argTypes);
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
    public SpyProcessor zorkaStats(String mbsName, String beanName, String attrName, String keyExpr,
                                   Object tstampField, Object timeField) {
        return new ZorkaStatsCollector(mbsName, beanName, attrName, keyExpr, slot(tstampField), slot(timeField));
    }


    public SpyProbe fetchArg(int arg) {
        return new SpyProbe(arg);
    }


    public SpyProbe fetchClass(String className) {
        return new SpyProbeElement(className);
    }


    public SpyProbe fetchException() {
        return new SpyProbe(FETCH_ERROR);
    }


    public SpyProbe fetchNull() {
        return new SpyProbe(FETCH_NULL);
    }


    public SpyProbe fetchRetVal() {
        return new SpyProbe(FETCH_RETVAL);
    }


    public SpyProbe fetchThread() {
        return new SpyProbe(FETCH_THREAD);
    }


    public SpyProbe fetchTime() {
        return new SpyProbe(FETCH_TIME);
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
    public SpyProcessor zorkaStat(String mbsName, String beanName, String attrName, Object tstampField, Object timeField) {
        return new JmxAttrCollector(mbsName, beanName, attrName, slot(tstampField), slot(timeField));
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
    public SpyProcessor callingCollector(This ns, String func) {
        return new CallingObjCollector(ns, func);
    }


    /**
     * Instructs spy to submit data to a collect() function in a BSH namespace..
     *
     * @param ns BSH namespace
     *
     * @return augmented spy definition
     */
    public SpyProcessor bshCollector(String ns) {
        return new CallingBshCollector(ns);
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
    public SpyProcessor fileCollector(FileTrapper trapper, String expr, ZorkaLogLevel logLevel) {
        return new FileCollector(trapper, expr, logLevel, "");
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
    public SpyProcessor getterCollector(String mbsName, String beanName, String attrName, String desc, Object src, Object...path) {
        return new GetterPresentingCollector(mbsName, beanName, attrName, desc, slot(src), path);
    }


    /**
     * Sends collected records to another SpyDefinition chain. Records will be processed by all
     * processors from ON_COLLECT chain and sent to collectors attached to it.
     *
     * @param sdef
     *
     * @return
     */
    public SpyProcessor sdefCollector(SpyDefinition sdef) {
        return new DispatchingCollector(sdef);
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
    public SpyProcessor snmpCollector(SnmpTrapper trapper, String oid, int spcode, TrapVarBindDef...bindings) {
        return new SnmpCollector(trapper, oid, SnmpLib.GT_SPECIFIC, spcode, oid, bindings);
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
    public SpyProcessor syslogCollector(SyslogTrapper trapper, String expr, int severity, int facility, String hostname, String tag) {
        return new SyslogCollector(trapper, expr, severity, facility, hostname, tag);
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
    public SpyProcessor zabbixCollector(ZabbixTrapper trapper, String expr, String key) {
        return new ZabbixCollector(trapper, expr, null, key);
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
    public SpyProcessor zabbixCollector(ZabbixTrapper trapper, String expr, String host, String key) {
        return new ZabbixCollector(trapper,  expr,  host,  key);
    }

    /**
     *
     * @param src
     * @param processor
     * @return
     */
    public SpyProcessor logAdapterCollector(LogProcessor processor, Object src) {
        return new LogAdaptingCollector(slot(src), processor);
    }


    /**
     *
     * @param level
     * @param msgTmpl
     * @return
     */
    public SpyProcessor logFormatCollector(LogProcessor processor, String level, String msgTmpl) {
        return new LogFormattingCollector(processor, level, msgTmpl);
    }


    /**
     *
     * @param processor
     * @param levelTmpl
     * @param msgTmpl
     * @param classTmpl
     * @param methodTmpl
     * @param excTmpl
     * @return
     */
    public SpyProcessor logFormatCollector(LogProcessor processor, String levelTmpl, String msgTmpl, String classTmpl,
                                           String methodTmpl, String excTmpl) {
        return new LogFormattingCollector(processor,  levelTmpl, msgTmpl, classTmpl, methodTmpl, excTmpl);
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
    public SpyProcessor format(Object dst, String expr) {
        return new StringFormatProcessor(slot(dst), expr);
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
    public SpyProcessor regexFilter(Object src, String regex) {
        return new RegexFilterProcessor(slot(src), regex);
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
    public SpyProcessor regexFilter(Object src, String regex, boolean filterOut) {
        return new RegexFilterProcessor(slot(src), regex, filterOut);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param src
     *
     * @param dst
     *
     * @param regex
     *
     * @param expr
     *
     * @return
     */
    public SpyProcessor transform(Object src, Object dst, String regex, String expr) {
        return transform(src, dst, regex, expr, false);
    }


    /**
     * Transforms data using regular expression and substitution.
     *
     * @param src
     *
     * @param dst
     *
     * @param regex
     *
     * @param expr
     *
     * @return
     */
    public SpyProcessor transform(Object src, Object dst, String regex, String expr, boolean filterOut) {
        return new RegexFilterProcessor(slot(src), slot(dst), regex, expr, filterOut);
    }


    /**
     * Normalizes a query string from src and puts result into dst.
     *
     * @param src
     * @param dst
     * @param normalizer
     * @return
     */
    public SpyProcessor normalize(Object src, Object dst, Normalizer normalizer) {
        return new NormalizingProcessor(slot(src), slot(dst), normalizer);
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
    public SpyProcessor get(Object src, Object dst, Object...path) {
        return new GetterProcessor(slot(src), slot(dst), path);
    }


    /**
     *
     * @param slot
     * @param threadLocal
     * @return
     */
    public SpyProcessor tlGet(Object slot, ThreadLocal<Object> threadLocal, Object...path) {
        return new ThreadLocalProcessor(slot(slot), ThreadLocalProcessor.GET, threadLocal, path);
    }


    /**
     *
     * @param slot
     * @param val
     * @return
     */
    public SpyProcessor put(Object slot, Object val) {
        return new ConstPutProcessor(slot(slot), val);
    }


    /**
     *
     * @param slot
     * @param threadLocal
     * @return
     */
    public SpyProcessor tlSet(Object slot, ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(slot(slot), ThreadLocalProcessor.SET, threadLocal);
    }


    /**
     *
     * @param threadLocal
     * @return
     */
    public SpyProcessor tlRemove(ThreadLocal<Object> threadLocal) {
        return new ThreadLocalProcessor(slot(0), ThreadLocalProcessor.REMOVE, threadLocal);
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
    public SpyProcessor tdiff(Object tstart, Object tstop, Object dst) {
        return new TimeDiffProcessor(slot(tstart), slot(tstop), slot(dst));
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
    public SpyProcessor call(Object src, Object dst, String methodName, Object... methodArgs) {
        return new MethodCallingProcessor(slot(src), slot(dst), methodName, methodArgs);
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
    public SpyProcessor ifSlotCmp(Object a, int op, Object b) {
        return ComparatorProcessor.scmp(slot(a), op, slot(b));
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
    public SpyProcessor ifValueCmp(Object a, int op, Object v) {
        return ComparatorProcessor.vcmp(slot(a), op, v);
    }


    public static Map<Character,Integer> stages = ZorkaUtil.constMap(
            'E', ON_ENTER, 'e', ON_ENTER,
            'R', ON_RETURN, 'r', ON_RETURN,
            'X', ON_ERROR, 'x', ON_ERROR,
            'S', ON_SUBMIT, 's', ON_SUBMIT,
            'C', ON_COLLECT, 's', ON_COLLECT
    );


    public static int[] slot(Object v) {
        if (v instanceof Integer) {
            return new int[] {-1, (Integer)v};
        } else if (v instanceof String) {
            String s = (String)v;
            Integer stage = stages.get(s.charAt(0));
            if (stage == null) {
                throw new IllegalArgumentException("Invalid stage. Try using one of characters: EeRrXxSsCc");
            }
            return new int[] { stage, Integer.parseInt(s.substring(1))};
        } else {
            throw new IllegalArgumentException("Illegal slot argument: " + v);
        }
    } // slot()

    public static int fs(int a, int b) {
        return a != -1 ? a : b;
    }
}
