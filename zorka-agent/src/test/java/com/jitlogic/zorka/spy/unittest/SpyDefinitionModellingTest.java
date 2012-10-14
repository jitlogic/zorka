package com.jitlogic.zorka.spy.unittest;

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

import bsh.This;
import com.jitlogic.zorka.spy.SpyDefinition;
import junit.framework.Assert;
import org.junit.Test;

/**
 * This is API modelling exercise rather then a real unit test.
 *
 * Some tests checking
 */
public class SpyDefinitionModellingTest {

    /**
     * Simple instrumenting of a single method. Only method with no arguments will be chosen.
     */
    //@Test
    public void testDefineSimpleInstrumentation() {
        SpyDefinition sdef =
            SpyDefinition.instrument("com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod", SpyDefinition.NO_ARGS)
                    .toStats("java", "some.app:type=ZorkaStats,name=SomeClass", "stats");
    }

    /**
     * Instrumenting method calls in multiple beans. Each bean will represent a single class.
     * Test mask correctness by the way.
     */
    @Test
    public void testDefineInstrumentationWithAntMasks() {
        SpyDefinition sdef =
            SpyDefinition.instrument("com.jitlogic.zorka.spy.**", "*")
                .toStats("java", "some.app:type=ZorkaStats,name=${className}", "stats");
    }

    /**
     * Instrumenting method calls and report method stats to multiple attributes of a single bean.
     */
    @Test
    public void testInstrumentMethodWithArgProcAndTestAndShortMask() {
        SpyDefinition sdef =
            SpyDefinition.instrument("com.jitlogic.zorka.spy.*", "*")
                .toStats("java", "some.app:type=ZorkaStats,name=${className}", "${methodName}");
    }


    /**
     * Instrumenting method calls with argument fetch and formatting. HTTP requests by exact URL in this example.
     */
    //@Test
    public void testInstrumentWithFormatArgs() {
        SpyDefinition sdef =
            SpyDefinition.instrument("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat("${1.request.requestURI}")
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI");
    }


    /**
     * Example: Instrument HTTP requests and cut off request arguments (if any)
     */
    //@Test
    public void testInstrumentWithFormatArgsAndTransformViaMethod() {
        SpyDefinition sdef =
            SpyDefinition.instrument("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat("${1.request.requestURI}")
                .transform(0, "split", "\\?").get(0, 0)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI");
    }


    /**
     * Example: Instrument HTTP requests with transforming function defined in BSH
     */
    //@Test
    public void testInstrumentWithCallRawArgsAndBshTransformingFunction() {
        This ns = null; // Use 'this' keyword in BSH
        SpyDefinition sdef =
            SpyDefinition.instrument("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withArguments(1).transform(ns, "classify_uris")
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byClass");
    }


    /**
     * Example: Instrument HTTP replies and collect statistics on return code (200, 404, etc.).
     */
    //@Test
    public void testInstrumentWithCatchArgsOnExit() {
        SpyDefinition sdef =
            SpyDefinition.instrument("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat("${2.reply.replyCode}").onExit()
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byCode");
    }


    /**
     * Example: Instrument HTTP requests, sort them by path and
     */
    //@Test
    public void testInstrumentTomcatWithPathAndCode() {
        SpyDefinition sdef =
            SpyDefinition.instrument("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat("${1.request.requestURI}", "${2.reply.replyCode}").onExit()
                .transform(0, "split", "\\?").get(0, 0)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests,httpCode=${1}", "stats", "${0}");
    }


    /**
     * Example: Instrument some function, grab return values and pass them to my_app.log_tst_count.
     * Not that withRetVal() implies onExit() and autmatically filters out exceptions.
     */
    public void testInstrumentAndGetReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument("com.jitlogic.zorka.spy.unittest.SomeClass", "getTstCount")
                .withRetVal().toBsh("someapp", "tstcount");

    }


    /**
     * Example: Instrument a function, grab both return value and an argument
     */
    //@Test
    public void testInstrumentGetSomeArgsAndReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument("com.jitlogic.zorka.spy.unittest.SomeClass", "otherMethod")
                .withRetVal().toBsh("someapp", "tstcount");
    }


    /**
     * Example: expose static method com.hp.ifc.net.mq.AppMessageQueue.getSize()
     * as 'size' attribute of 'hpsd:type=SDStats,name=AppMessageQueue' bean.
     * Intercept
     */
    public void testExposeStaticMethodFromSomeClassAtStartup() {
        SpyDefinition sdef =
            SpyDefinition.catchOnce("com.hp.ifc.bus.AppServer", "startup")
                .withClass("com.hp.ifc.net.mq.AppMessageQueue").withClassLoader()
                .toGetter("java", "hpsd:type=SDStats,name=AppMessageQueue", "size", "getSize()");
    }


    /**
     * Example: expose an object instance as a bean. Expose some values from object instance
     * via getters. Access to instance data will be synchronized. Multiple instances can be catched.
     * Note that "catched" object won't be garbage collected anymore !
     */
    public void testExposeSomeStaticMethodsOfAnObject() {
        SpyDefinition sdef =
            SpyDefinition.catchEvery("some.package.SomeBean", SpyDefinition.CONSTRUCTOR)
                .withArguments(0).withClassLoader()
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "count", "getCount()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "backlog", "getBacklog()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "time", "getProcessingTime()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "url", "getUrl()")
                .synchronizedWith(0);
    }

    /**
     * Example: register JBoss MBean Server. Each withXXX() call appends new
     * values to parameter list. In this example object instance will be first
     * argument and current class loader will be a second
     */
    //@Test
    public void testRegisterJBossMBeanServer() {
        SpyDefinition.catchOnce("org.jboss.mx.MBeanServerImpl", SpyDefinition.CONSTRUCTOR)
           .withFormat("jboss").withArguments(0).withClassLoader()
           .toBsh("zorka", "registerMBeanServer");
    }


    /**
     * Example: expose some object attribute directly (in this example: hash map)
     */
    //@Test
    public void testExposeSomeHashMapAsMBeanAttribute() {
        SpyDefinition sdef =
            SpyDefinition.catchOnce("some.package.SingletonBean", SpyDefinition.CONSTRUCTOR)
                .withArguments(0).get(0, "someMap")
                .toAttr("java", "SomeApp:type=SingletonType", "map");
    }
}
