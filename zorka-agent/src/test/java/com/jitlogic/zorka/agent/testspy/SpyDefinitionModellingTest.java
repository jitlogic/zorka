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

import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.spy.SpyMatcher;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.SpyProbeElement;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

import static com.jitlogic.zorka.spy.SpyConst.*;
import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 * This is an API modelling exercise rather then a real unit test.
 *
 * @author Rafal Lewczuk
 *
 */
public class SpyDefinitionModellingTest extends ZorkaFixture {


    @Test
    public void testDefineEmptySpyDef() throws Exception {
        SpyDefinition sdef = SpyDefinition.instance();

        assertNotNull(sdef.getMatchers());
        assertEquals(0, sdef.getMatchers().size());

        assertNotNull(sdef.getProbes(ON_ENTER));
        assertEquals(0, sdef.getProbes(ON_ENTER).size());

        assertNotNull(sdef.getProcessors(ON_RETURN));
        assertEquals(0, sdef.getProcessors(ON_RETURN).size());
    }


    @Test
    public void testDefineTrivialInstrumentingDef() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument();

        List<SpyProbeElement> probes1 = sdef.getProbes(ON_ENTER);
        assertEquals(1, probes1.size());
        assertEquals(FETCH_TIME, probes1.get(0).getArgType());


        List<SpyProbeElement> probes2 = sdef.getProbes(ON_RETURN);
        assertEquals(1, probes2.size());
        assertEquals(FETCH_TIME, probes2.get(0).getArgType());

        List<SpyProbeElement> probes3 = sdef.getProbes(ON_ERROR);
        assertEquals(1, probes3.size());
        assertEquals(FETCH_TIME, probes3.get(0).getArgType());
    }


    @Test
    public void testCheckProperStateInTrivialInstrumentationAndTryExtension() throws Exception {
        SpyDefinition sdef = SpyDefinition.instrument().onEnter("com.jitlogic.test.SomeClass");

        List<SpyProbeElement> probeElements = sdef.getProbes(ON_ENTER);
        assertEquals(2, probeElements.size());
        assertEquals(FETCH_CLASS, probeElements.get(1).getArgType());
        assertEquals("com.jitlogic.test.SomeClass", probeElements.get(1).getClassName());



    }


    @Test
    public void testSwitchStageAndCheckImmutability() throws Exception {
        SpyDefinition sdef1 = SpyDefinition.instrument();

        SpyDefinition sdef2 = sdef1.onReturn(FETCH_ERROR);
        List<SpyProbeElement> probes2 = sdef2.getProbes(ON_RETURN);

        assertEquals(1, sdef1.getProbes(ON_RETURN).size());
        assertEquals(2, sdef2.getProbes(ON_RETURN).size());
    }


    /**
     * Simple instrumenting of a single method. Only method with no arguments will be chosen.
     */
    @Test
    public void testDefineSimpleInstrumentation() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod(SpyMatcher.DEFAULT_FILTER,
                    "com.jitlogic.zorka.spy.unittest.SomeClass", "someMethod", SM_ANY_TYPE, SM_NOARGS))
                .onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "some.app:type=ZorkaStats,name=SomeClass", "stats", "${methodName}", 0, 1));
        assertEquals(1, sdef.getMatchers().size());
    }


    @Test
    public void testInstrumentWithGetterOnReturn() {
        SpyDefinition sdef = SpyDefinition.instance()
            .onEnter(FETCH_TIME)
            .onReturn(FETCH_TIME).onReturn(2, spy.get(1, 1, "response", "status"))
            .onSubmit(spy.tdiff(0,2,2));

        assertEquals(1, sdef.getProbes(ON_ENTER).size());
        assertEquals(2, sdef.getProbes(ON_RETURN).size());
        assertEquals(1, sdef.getProcessors(ON_SUBMIT).size());
    }


    /**
     * Instrumenting method calls in multiple beans. Each bean will represent a single class.
     * Test mask correctness by the way.
     */
    //@Test
    public void testDefineInstrumentationWithAntMasks() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("com.jitlogic.zorka.spy.**", "*")).onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "some.app:type=ZorkaStats,name=${className}", "stats", "${methodName}", 0, 1));
    }


    /**
     * Instrumenting method calls and report method stats to multiple attributes of a single bean.
     */
    //@Test
    public void testInstrumentMethodWithArgProcAndTestAndShortMask() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("com.jitlogic.zorka.spy.*", "*")).onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "some.app:type=ZorkaStats,name=${className}", "${methodName}", "method", 0, 1));
    }


    /**
     * Instrumenting method calls with argument fetch and formatting. HTTP requests by exact URL in this example.
     */
    //@Test
    public void testInstrumentWithFormatArgs() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
                .onEnter(spy.formatter(2, "${1.request.requestURI}")).onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI", "${2}", 0, 1));
    }


    /**
     * Example: Instrument HTTP requests and cut off request arguments (if any)
     */
    //@Test
    public void testInstrumentWithFormatArgsAndTransformViaMethod() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
                .onEnter(spy.formatter(2, "${1.request.requestURI}"), spy.call(0, 0, "split", "\\?"), spy.get(0, 0))
                .onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byURI", "${methodName}", 0, 1));
    }



    /**
     * Example: Instrument HTTP replies and submit statistics on return code (200, 404, etc.).
     */
    //@Test
    public void testInstrumentWithCatchArgsOnExit() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
                .onEnter(2, spy.formatter(1, "${0.reply.replyCode}")).onSubmit(spy.tdiff(0, 2, 2))
                .onCollect(spy.zorkaStats("java", "Catalina:type=ZorkaStats,name=HttpRequests", "byCode", "${1}", 0, 2));
    }


    /**
     * Example: Instrument HTTP requests, sort them by path and
     */
    //@Test
    public void testInstrumentTomcatWithPathAndCode() {
        SpyDefinition sdef =
            SpyDefinition.instrument().include(spy.byMethod("org.apache.catalina.core.StandardEngineValve", "invoke"))
                .onReturn(1,2,
                    spy.formatter(1, "${1.request.requestURI}"),
                    spy.formatter(2, "${2.reply.replyCode}"),
                    spy.call(1, 1, "split", "\\?"),
                    spy.get(1, 0))
                .onSubmit(spy.tdiff(0, 1, 1))
                .onCollect(spy.zorkaStats("java", "Catalina:type=ZorkaStats,name=HttpRequests,httpCode=${1}", "stats", "${0}", 0, 1));
    }


    /**
     * Example: Instrument some function, grab return values and pass them to my_app.log_tst_count.
     * Not that withRetVal() implies onReturn() and autmatically filters out exceptions.
     */
    public void testInstrumentAndGetReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument()
                .include(spy.byMethod("com.jitlogic.zorka.spy.unittest.SomeClass", "getTstCount"))
                .onReturn(FETCH_RETVAL).onCollect(spy.bshCollector("someapp"));

    }


    /**
     * Example: Instrument a function, grab both return value and an argument
     */
    //@Test
    public void testInstrumentGetSomeArgsAndReturnValue() {
        SpyDefinition sdef =
            SpyDefinition.instrument()
                .include(spy.byMethod("com.jitlogic.zorka.spy.unittest.SomeClass", "otherMethod"))
                .onReturn(FETCH_RETVAL).onCollect(spy.bshCollector("someapp"));
    }


    /**
     * Example: expose static method com.hp.ifc.net.mq.AppMessageQueue.getSize()
     * as 'size' attribute of 'hpsd:type=SDStats,name=AppMessageQueue' bean.
     * Intercept
     */
    public void testExposeStaticMethodFromSomeClassAtStartup() {
        SpyDefinition sdef =
            SpyDefinition.instance().include(spy.byMethod("com.hp.ifc.bus.AppServer", "startup"))
                .onEnter("com.hp.ifc.net.mq.AppMessageQueue")
                .onCollect(spy.getterCollector("java", "hpsd:type=SDStats,name=AppMessageQueue", "size", "meh", slot(0), "getSize()"));
    }


    /**
     * Example: expose an object instance as a bean. Expose some values from object instance
     * via getters. Access to instance data will be synchronized. Multiple instances can be catched.
     * Note that "catched" object won't be garbage collected anymore !
     */
    public void testExposeSomeStaticMethodsOfAnObject() {
        SpyDefinition sdef =
            SpyDefinition.instance().include(spy.byMethod("some.package.SomeBean", SM_CONSTRUCTOR))
                .onEnter(0)
                .onCollect(
                    spy.getterCollector("java", "SomeApp:type=SomeType,name=${0.name}", "count", "meh", slot(0), "getCount()"),
                    spy.getterCollector("java", "SomeApp:type=SomeType,name=${0.name}", "backlog", "meh", slot(0), "getBacklog()"),
                    spy.getterCollector("java", "SomeApp:type=SomeType,name=${0.name}", "time", "meh", slot(0), "getProcessingTime()"),
                    spy.getterCollector("java", "SomeApp:type=SomeType,name=${0.name}", "url", "meh", slot(0), "getUrl()"));
    }

    /**
     * Example: register JBoss MBean Server. Each withXXX() call appends new
     * values to parameter list. In this example object instance will be first
     * argument and current class loader will be a second
     */
    //@Test
    public void testRegisterJBossMBeanServer() {
        SpyDefinition.instance().include(spy.byMethod("org.jboss.mx.MBeanServerImpl", SM_CONSTRUCTOR))
           .onEnter(spy.formatter(0, "jboss")).onEnter(0, FETCH_THREAD)
           .onCollect(spy.bshCollector("jboss.register"));
    }


    /**
     * Example: expose some object attribute directly (in this example: hash map)
     */
    //@Test
    public void testExposeSomeHashMapAsMBeanAttribute() {
        SpyDefinition sdef =
            SpyDefinition.instance().include(spy.byMethod("some.package.SingletonBean", SM_CONSTRUCTOR))
                .onEnter(0, spy.get(0, 0, "someMap"))
                .onCollect(spy.getterCollector("java", "SomeApp:type=SingletonType", "map", "Some map", slot(0)));
    }
}
