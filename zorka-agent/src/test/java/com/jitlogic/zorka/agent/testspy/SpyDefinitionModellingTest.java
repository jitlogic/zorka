package com.jitlogic.zorka.agent.testspy;

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

import bsh.This;
import com.jitlogic.zorka.spy.SpyMatcher;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyProbeElement;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

import static com.jitlogic.zorka.spy.SpyConst.*;

/**
 * This is an API modelling exercise rather then a real unit test.
 *
 * @author Rafal Lewczuk
 *
 */
public class SpyDefinitionModellingTest {


    @Test
    public void testDefineEmptySpyDef() throws Exception {
        SpyDefinition sdef = SpyDefinition.newInstance();

        assertNotNull(sdef.getCollectors());
        assertEquals(0, sdef.getCollectors().size());

        assertNotNull(sdef.getMatchers());
        assertEquals(0, sdef.getMatchers().size());

        assertNotNull(sdef.getProbes(ON_ENTER));
        assertEquals(0, sdef.getProbes(ON_ENTER).size());

        assertNotNull(sdef.getTransformers(ON_EXIT));
        assertEquals(0, sdef.getTransformers(ON_EXIT).size());
    }


    @Test
    public void testDefineTrivialInstrumentingDef() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument();

        List<SpyProbeElement> probes1 = sdef.getProbes(ON_ENTER);
        assertEquals(1, probes1.size());
        assertEquals(SpyProbeElement.FETCH_TIME, probes1.get(0).getArgType());


        List<SpyProbeElement> probes2 = sdef.getProbes(ON_EXIT);
        assertEquals(1, probes2.size());
        assertEquals(SpyProbeElement.FETCH_TIME, probes2.get(0).getArgType());

        List<SpyProbeElement> probes3 = sdef.getProbes(ON_ERROR);
        assertEquals(1, probes3.size());
        assertEquals(SpyProbeElement.FETCH_TIME, probes3.get(0).getArgType());
    }


    @Test
    public void testCheckProperStateInTrivialInstrumentationAndTryExtension() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument().withClass("com.jitlogic.test.SomeClass");

        List<SpyProbeElement> probeElements = sdef.getProbes(ON_ENTER);
        assertEquals(2, probeElements.size());
        assertEquals(SpyProbeElement.FETCH_CLASS, probeElements.get(1).getArgType());
        assertEquals("com.jitlogic.test.SomeClass", probeElements.get(1).getClassName());



    }


    @Test
    public void testSwitchStageAndCheckImmutability() throws Exception {
        SpyDefinition sdef1 = SpyDefinition.instrument();

        SpyDefinition sdef2 = sdef1.onExit().withError();
        List<SpyProbeElement> probes2 = sdef2.getProbes(ON_EXIT);

        assertEquals(1, sdef1.getProbes(ON_EXIT).size());
        assertEquals(2, sdef2.getProbes(ON_EXIT).size());
    }


    /**
     * Simple instrumenting of a single method. Only method with no arguments will be chosen.
     */
    @Test
    public void testDefineSimpleInstrumentation() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor(SpyMatcher.DEFAULT_FILTER,
                "com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod", SM_ANY_TYPE, SM_NOARGS)
                .onSubmit().timeDiff(0,1,1)
                .toStats("java", "some.app:type=ZorkaStats,name=SomeClass", "stats", "${methodName}", 0, 1);
        assertEquals(1, sdef.getMatchers().size());
    }


    /**
     * Instrumenting method calls in multiple beans. Each bean will represent a single class.
     * Test mask correctness by the way.
     */
    //@Test
    public void testDefineInstrumentationWithAntMasks() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("com.jitlogic.zorka.spy.**", "*").onSubmit().timeDiff(0,1,1)
                .toStats("java", "some.app:type=ZorkaStats,name=${className}", "stats", "${methodName}", 0,1);
    }


    /**
     * Instrumenting method calls and report method stats to multiple attributes of a single bean.
     */
    //@Test
    public void testInstrumentMethodWithArgProcAndTestAndShortMask() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("com.jitlogic.zorka.spy.*", "*").onSubmit().timeDiff(0,1,1)
                .toStats("java", "some.app:type=ZorkaStats,name=${className}", "${methodName}", "method", 0, 1);
    }


    /**
     * Instrumenting method calls with argument fetch and formatting. HTTP requests by exact URL in this example.
     */
    //@Test
    public void testInstrumentWithFormatArgs() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat(2,"${1.request.requestURI}").onSubmit().timeDiff(0,1,1)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI", "${2}", 0, 1);
    }


    /**
     * Example: Instrument HTTP requests and cut off request arguments (if any)
     */
    //@Test
    public void testInstrumentWithFormatArgsAndTransformViaMethod() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withFormat(2,"${1.request.requestURI}").transform(0, "split", "\\?").get(0, 0)
                .onSubmit().timeDiff(0,1,1)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI", "${methodName}", 0, 1);
    }


    /**
     * Example: Instrument HTTP requests with transforming function defined in BSH
     */
    //@Test
    public void testInstrumentWithCallRawArgsAndBshTransformingFunction() {
        This ns = null; // Use 'this' keyword in BSH
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withArguments(1).transform(ns, "classify_uris").onSubmit().timeDiff(0,2,2)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byClass", "${1}", 0, 2);
    }


    /**
     * Example: Instrument HTTP replies and submit statistics on return code (200, 404, etc.).
     */
    //@Test
    public void testInstrumentWithCatchArgsOnExit() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("org.apache.catalina.core.StandardEngineValve", "invoke")
                .withArguments(2).withFormat(1,"${0.reply.replyCode}").onSubmit().timeDiff(0,2,2)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byCode", "${1}", 0, 2);
    }


    /**
     * Example: Instrument HTTP requests, sort them by path and
     */
    //@Test
    public void testInstrumentTomcatWithPathAndCode() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("org.apache.catalina.core.StandardEngineValve", "invoke")
                .onExit().withArguments(1,2)
                .withFormat(1,"${1.request.requestURI}").withFormat(2,"${2.reply.replyCode}")
                .transform(1, "split", "\\?").get(1, 0).onSubmit().timeDiff(0, 1, 1)
                .toStats("java", "Catalina:type=ZorkaStats,name=HttpRequests,httpCode=${1}", "stats", "${0}", 0, 1);
    }


    /**
     * Example: Instrument some function, grab return values and pass them to my_app.log_tst_count.
     * Not that withRetVal() implies onExit() and autmatically filters out exceptions.
     */
    public void testInstrumentAndGetReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("com.jitlogic.zorka.spy.unittest.SomeClass", "getTstCount")
                .withRetVal().toBsh("someapp", "tstcount");

    }


    /**
     * Example: Instrument a function, grab both return value and an argument
     */
    //@Test
    public void testInstrumentGetSomeArgsAndReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument().lookFor("com.jitlogic.zorka.spy.unittest.SomeClass", "otherMethod")
                .withRetVal().toBsh("someapp", "tstcount");
    }


    /**
     * Example: expose static method com.hp.ifc.net.mq.AppMessageQueue.getSize()
     * as 'size' attribute of 'hpsd:type=SDStats,name=AppMessageQueue' bean.
     * Intercept
     */
    public void testExposeStaticMethodFromSomeClassAtStartup() {
        SpyDefinition sdef =
            SpyDefinition.newInstance().once().lookFor("com.hp.ifc.bus.AppServer", "startup")
                .withClass("com.hp.ifc.net.mq.AppMessageQueue")
                .toGetter("java", "hpsd:type=SDStats,name=AppMessageQueue", "size", "getSize()");
    }


    /**
     * Example: expose an object instance as a bean. Expose some values from object instance
     * via getters. Access to instance data will be synchronized. Multiple instances can be catched.
     * Note that "catched" object won't be garbage collected anymore !
     */
    public void testExposeSomeStaticMethodsOfAnObject() {
        SpyDefinition sdef =
            SpyDefinition.newInstance().once().lookFor("some.package.SomeBean", SM_CONSTRUCTOR)
                .withArguments(0)
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "count", "getCount()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "backlog", "getBacklog()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "time", "getProcessingTime()")
                .toGetter("java", "SomeApp:type=SomeType,name=${0.name}", "url", "getUrl()");
    }

    /**
     * Example: register JBoss MBean Server. Each withXXX() call appends new
     * values to parameter list. In this example object instance will be first
     * argument and current class loader will be a second
     */
    //@Test
    public void testRegisterJBossMBeanServer() {
        SpyDefinition.newInstance().once().lookFor("org.jboss.mx.MBeanServerImpl", SM_CONSTRUCTOR)
           .withFormat(0, "jboss").withArguments(0).withThread()
           .toBsh("zorka", "registerMBeanServer");
    }


    /**
     * Example: expose some object attribute directly (in this example: hash map)
     */
    //@Test
    public void testExposeSomeHashMapAsMBeanAttribute() {
        SpyDefinition sdef =
            SpyDefinition.newInstance().once().lookFor("some.package.SingletonBean", SM_CONSTRUCTOR)
                .withArguments(0).get(0, 0, "someMap")
                .toGetter("java", "SomeApp:type=SingletonType", "map");
    }
}
