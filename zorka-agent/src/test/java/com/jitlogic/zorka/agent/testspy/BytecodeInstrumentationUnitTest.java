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
package com.jitlogic.zorka.agent.testspy;

import com.jitlogic.zorka.agent.testspy.support.TestSpyTransformer;
import com.jitlogic.zorka.agent.testspy.support.TestSubmitter;
import com.jitlogic.zorka.agent.testutil.ZorkaFixture;
import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.spy.MainSubmitter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.spy.SpyLib.*;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.*;

public class BytecodeInstrumentationUnitTest extends ZorkaFixture {

    public final static String TCLASS1 = "com.jitlogic.zorka.agent.testspy.support.TestClass1";
    public final static String TCLASS2 = "com.jitlogic.zorka.agent.testspy.support.TestClass2";
    public final static String TACLASS = "com.jitlogic.zorka.agent.testspy.support.ClassAnnotation";

    private TestSpyTransformer engine;
    private TestSubmitter submitter;


    @Before
    public void setUp() throws Exception {
        engine = new TestSpyTransformer();
        submitter = new TestSubmitter();
        MainSubmitter.setSubmitter(submitter);
    }


    @After
    public void tearDown() throws Exception {
        MainSubmitter.setSubmitter(null);
    }


    @Test
    public void testClassWithoutAnyTransform() throws Exception{
        Object obj = instantiate(engine, TCLASS1);
        assertNotNull(obj);
        assertEquals(TCLASS1, obj.getClass().getName());
    }


    @Test
    public void testTrivialInstrumentOnlyEntryPointWithThisRef() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(0, "E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(obj, submitter.get(0).get(0));
    }


//    @Test  TODO make this test working
    public void testStaticNotPublicMethod() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchConst(null, "R0"))
                .include(spy.byMethod(TCLASS1, "nonPublicStatic")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "nonPublicStatic");

        assertEquals(1, submitter.size());
    }

    @Test
    public void testInstrumentWithNullProbe() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchConst(null, "E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(null, submitter.get(0).get(0));
    }

    @Test
    public void testInstrumentWithConstProbe() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchConst(42L, "E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        //assertEquals(42L, submitter.get(0).get(0));  TODO this test has to be performed with DispatchingSubmitter fully implemented (or: use feed() method directly
    }

    //@Test  TODO make this test working
    public void testInstrumentWithNoProbes() throws Exception {
        engine.add(SpyDefinition.instance().onEnter().include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(0, submitter.get(0).size());
    }

    @Test
    public void testTrivialInstrumentOnlyEntryPointWithCurrentTime() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch entry point", 1, submitter.size());
        assertTrue("should return Long", submitter.get(0).get(0) instanceof Long);

    }


    @Test
    public void testInstrumentWithTimeOnEnterExit() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch both entry and exit points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTimeOnEnterError() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "errorMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "errorMethod");

        assertEquals("should catch both entry and error points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTwoProbes() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "trivialMethod")));
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(0, "E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should submit 3 times. ", 3, submitter.size());

        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertEquals("should return object instance itself", obj, submitter.get(1).get(0));
        assertTrue("should pass Long", submitter.get(2).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentConstructorWithTime() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(2, submitter.size());
    }


    @Test
    public void testInstrumentConstructorWithSelfRef() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchArg(0, "R0"))
                .include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(1, submitter.size());
        assertEquals("should return object itself", obj, submitter.get(0).get(0));
    }


    @Test
    public void testInstrumentConstructorWithInvalidSelfRefOnBeginning() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(0, "E0"))
                .include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(1, submitter.size());
        //assertEquals("should return object itself", obj, submitter.get(0).get(0));
        assertNull("should return null instead of ", submitter.get(0).get(0));
    }


    @Test
    public void testFetchClassFromInstrumentedCode() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchClass(TCLASS1, "E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertTrue("Fetched object is a class", submitter.get(0).get(0) instanceof Class);
        assertEquals(TCLASS1, ((Class)(submitter.get(0).get(0))).getName());
    }


    @Test
    public void testFetchIntegerTypeArgument() throws Exception {
        engine.add(SpyDefinition.instance()
            .onEnter(spy.fetchArg(1, "E0"), spy.fetchArg(2, "E1"), spy.fetchArg(3, "E2"), spy.fetchArg(4, "E3"))
            .include(spy.byMethod(TCLASS1, "paramMethod1")));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod1", 10, 20L, (short) 30, (byte) 40));

        assertEquals(1, submitter.size());
        assertEquals(Integer.valueOf(10), submitter.get(0).get(0));
        assertEquals(Long.valueOf(20), submitter.get(0).get(1));
        assertEquals((short)30, submitter.get(0).get(2));
        assertEquals((byte)40, submitter.get(0).get(3));
    }


    @Test
    public void testFetchBooleanCharTypeArgument() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(1, "E0"), spy.fetchArg(2, "E1"))
                .include(spy.byMethod(TCLASS1, "paramMethod2")));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod2", true, 'A'));
        assertEquals(1, submitter.size());
        assertEquals(true, submitter.get(0).get(0));
        assertEquals('A', submitter.get(0).get(1));
    }


    @Test
    public void testFetchFloatingPointArgs() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(1, "E0"), spy.fetchArg(2, "E1"))
                .include(spy.byMethod(TCLASS1, "paramMethod3")));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod3", 1.23, (float) 2.34));
        assertEquals(1, submitter.size());
        assertEquals(1.23, (Double)(submitter.get(0).get(0)), 0.01);
        assertEquals(2.34, (Float)(submitter.get(0).get(1)), 0.01);
    }


    @Test
    public void testCheckImmediateFlagInEntryPointOnlyProbe() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertEquals(SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testCheckNoFlagOnEnterAndFlushFlagOnExit() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);
    }


    @Test
    public void testCheckNoProbeOnEnterAndImmediateFlagOnExit() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchTime("R0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertEquals(SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testNoProbeOnExitButProbeOnErrorAndOnEnter() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchTime("E0")).onError(spy.fetchTime("X0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);
        assertTrue("should pass no values", submitter.get(1).nullVals());
    }


    @Test
    public void testNoProbeOnErrorButProbeOnEnterAndExit() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchTime("E0")).onReturn(spy.fetchTime("X0"))
                .include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);
        assertTrue("should pass no values", submitter.get(1).nullVals());
    }


    @Test
    public void testIfContextIsTheSameForTheSameClassLoadedTwice() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj1 = instantiate(engine, TCLASS1);
        Object obj2 = instantiate(engine, TCLASS1);

        checkForError(invoke(obj1, "trivialMethod"));
        checkForError(invoke(obj2, "trivialMethod"));

        assertEquals(2, submitter.size());
        assertEquals("context IDs should be the same", submitter.get(0).id, submitter.get(1).id);
    }


    @Test
    public void testSubmissionOrderWhenMoreThanOneProbeInOneMethodRet() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "trivialMethod")));
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(4, submitter.size());

        assertEquals("first and last submit should have the same context ID:",
                submitter.get(0).id, submitter.get(3).id);

        assertEquals("second and third submit should have the same context ID:",
                submitter.get(1).id, submitter.get(2).id);
    }


    @Test
    public void testSubmissionOrderWhenMoreThanOneProbeInOneMethodErr() throws Exception {
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "errorMethod")));
        engine.add(SpyDefinition.instrument().include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals(4, submitter.size());

        assertEquals("first and last submit should have the same context ID:",
                submitter.get(0).id, submitter.get(3).id);

        assertEquals("second and third submit should have the same context ID:",
                submitter.get(1).id, submitter.get(2).id);
    }


    @Test
    public void testFetchArraysOfSimpleTypes() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg(1, "E0"), spy.fetchArg(2, "E1"), spy.fetchArg(3, "E2"))
                .include(spy.byMethod(TCLASS1, "paramMethod4")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod4", new int[]{1,2,3}, new byte[]{5,6,7}, new double[]{7,8,9}));

        assertEquals(1, submitter.size());
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(0) instanceof int[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(1) instanceof byte[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(2) instanceof double[]);
    }


    @Test
    public void testFetchReturnObject() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "strMethod")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "strMethod"));

        assertEquals(1, submitter.size());
        assertEquals("oja!", submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnSimpleTypeObject() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "getUltimateQuestionOfLife")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionOfLife"));

        assertEquals(1, submitter.size());
        assertEquals(42, submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnValWithDirtyVariableStack() throws Exception {
        engine.add(SpyDefinition.instance().onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "getUltimateQuestionWithLocalVars")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionWithLocalVars"));

        assertEquals(1, submitter.size());
        assertEquals(42, submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchCurrentThreadSubmission() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchThread("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(Thread.currentThread(), submitter.get(0).get(0));
    }


    @Test
    public void testFetchExceptionObject() throws Exception {
        engine.add(SpyDefinition.instance().onError(spy.fetchException("X0"))
                .include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        Object err = invoke(obj, "errorMethod");

        // Check what instrumentation catched
        assertEquals(1, submitter.size());
        assertEquals(err, submitter.get(0).get(0));
        assertTrue("Should return an exception.", submitter.get(0).get(0) instanceof NullPointerException);
        assertEquals("dUP!", ((Exception)submitter.get(0).get(0)).getMessage());
    }


    @Test
    public void testMatchClassByAnnotation() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchConst(null, "X"))
                .include(spy.byAnnotation(TACLASS)));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
    }


    @Test
    public void testNonMatchClassByAnnotation() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchConst(null, "X"))
                .include(spy.byAnnotation(TACLASS)));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "trivialMethod");

        assertEquals(0, submitter.size());
    }

    // TODO test if stack traces in exceptions are the same with and without intercepting errors by instrumentation

    // TODO test instrumenting static method

    // TODO test instrumenting static constructor

    // TODO check if stack traces for instrumented and non-instrumented method are the same if method throws an exception

}
