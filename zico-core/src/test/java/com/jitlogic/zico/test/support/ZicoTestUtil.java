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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.test.support;


import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.core.model.TraceInfoSearchQuery;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import java.util.Properties;
import java.util.Random;

public class ZicoTestUtil {

    private final static String[] traceNames = { "MY_TRACE" };
    private final static String[] classNames = { "my.pkg.SomeClass", "my.pkg.OtherClass", "other.pkg.AnotherClass" };
    private final static String[] methodNames = { "someMethod", "otherMethod", "anotherMethod", "yetAnotherMethod" };
    private final static String[] methodSignatures = { "()V" };

    public static SymbolRegistry symbols;
    public static MetricsRegistry metrics;

    private static Random random = new Random();
    private static long lastClock = 100L;

    public static KeyValuePair kv(String name, String val) {
        return new KeyValuePair(name, val);
    }


    public static TraceRecord trace(Object...args) {
        return traceP(
                randName(traceNames), randName(classNames), randName(methodNames), randName(methodSignatures),
                nextTime(500), args);
    }


    public static TraceRecord traceP(String traceName, String className, String methodName, String signature,
                                    long clock, Object...trs) {

        TraceRecord ret = rec(className, methodName, signature, trs);

        ret.setFlags(TraceRecord.TRACE_BEGIN);
        TraceMarker tm = new TraceMarker(ret,
                symbols.symbolId(traceName != null ? traceName : randName(traceNames)),
                clock);
        ret.setMarker(tm);

        return ret;
    }


    public static TraceRecord rec(Object...args) {
        return rec(randName(classNames), randName(methodNames), randName(methodSignatures), args);
    }


    private static TraceRecord rec(String className, String methodName, String methodSignature, Object[] args) {
        TraceRecord ret = new TraceRecord(null);

        ret.setClassId(symbols.symbolId(className != null ? className : randName(classNames)));
        ret.setMethodId(symbols.symbolId(methodName != null ? methodName : randName(methodNames)));
        ret.setSignatureId(symbols.symbolId(methodSignature != null ? methodSignature : randName(methodSignatures)));
        ret.setTime(randTime(10000000L));

        for (Object arg : args) {
            if (arg instanceof TraceRecord) {
                TraceRecord tr = (TraceRecord)arg;
                ret.addChild(tr);
                ret.setTime(ret.getTime()+tr.getTime());
            } else if (arg instanceof KeyValuePair) {
                KeyValuePair kvp = (KeyValuePair)arg;
                ret.setAttr(symbols.symbolId(kvp.getKey()), kvp.getValue());
            }
        }

        return ret;
    }


    public static long nextTime(long limit) {
        return lastClock += randTime(limit);
    }


    public static long randTime(long limit) {
        return (Math.abs(random.nextLong()) + 1000) % limit;
    }


    public static String randName(String...inputs) {
        return inputs[random.nextInt(inputs.length)];
    }

    public static String rClass() {
        return randName(classNames);
    }

    public static String rMethod() {
        return randName(methodNames);
    }

    public static String rSignature() {
        return randName(methodSignatures);
    }

    public static TraceInfoSearchQuery tiq(String hostName, int flags, String traceName, long minTime, String expr) {
        TraceInfoSearchQuery q = new TraceInfoSearchQuery();

        q.setHostName(hostName);
        q.setFlags(flags);
        q.setTraceName(traceName);
        q.setMinMethodTime(minTime);
        q.setSearchExpr(expr);
        q.setLimit(100);

        return q;
    }

    public static SymbolicException boo() {
        return new SymbolicException(new RuntimeException("BOO"), symbols, true);
    }

    public static Properties setProps(Properties props, String... data) {

        for (int i = 1; i < data.length; i += 2) {
            props.setProperty(data[i - 1], data[i]);
        }

        return props;
    }

}
