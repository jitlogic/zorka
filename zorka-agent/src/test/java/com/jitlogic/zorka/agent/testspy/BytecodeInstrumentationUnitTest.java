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

import com.jitlogic.zorka.spy.SpyLib;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.spy.SpyConst.*;
import static com.jitlogic.zorka.spy.SpyLib.*;

import static com.jitlogic.zorka.agent.testutil.JmxTestUtil.*;

public class BytecodeInstrumentationUnitTest extends ZorkaFixture {

    public final static String TCLASS1 = "com.jitlogic.zorka.agent.testspy.support.TestClass1";

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
        engine.add(SpyDefinition.instance().onEnter().withArguments(0).lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(obj, submitter.get(0).get(0));
    }


    @Test
    public void testTrivialInstrumentOnlyEntryPointWithCurrentTime() throws Exception {
        engine.add(SpyDefinition.instance().onEnter().withTime().lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch entry point", 1, submitter.size());
        assertTrue("should return Long", submitter.get(0).get(0) instanceof Long);

    }


    @Test
    public void testInstrumentWithTimeOnEnterExit() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch both entry and exit points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTimeOnEnterError() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "errorMethod"));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "errorMethod");

        assertEquals("should catch both entry and error points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTwoProbes() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "trivialMethod"));
        engine.add(SpyDefinition.instance().withArguments(0).lookFor(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should submit 3 times. ", 3, submitter.size());

        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertEquals("should return object instance itself", obj, submitter.get(1).get(0));
        assertTrue("should pass Long", submitter.get(2).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentConstructorWithTime() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, SM_CONSTRUCTOR));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(2, submitter.size());
    }


    @Test
    public void testInstrumentConstructorWithSelfRef() throws Exception {
        engine.add(SpyDefinition.instance().onReturn().withArguments(0).lookFor(TCLASS1, SM_CONSTRUCTOR));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(1, submitter.size());
        assertEquals("should return object itself", obj, submitter.get(0).get(0));
    }


    @Test
    public void testInstrumentConstructorWithInvalidSelfRefOnBeginning() throws Exception {
        engine.add(SpyDefinition.instance().withArguments(0).lookFor(TCLASS1, SM_CONSTRUCTOR));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);

        assertEquals(1, submitter.size());
        //assertEquals("should return object itself", obj, submitter.get(0).get(0));
        assertNull("should return null instead of ", submitter.get(0).get(0));
    }


    @Test
    public void testFetchClassFromInstrumentedCode() throws Exception {
        engine.add(SpyDefinition.instance().withClass(TCLASS1).lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertTrue("Fetched object is a class", submitter.get(0).get(0) instanceof Class);
        assertEquals(TCLASS1, ((Class)(submitter.get(0).get(0))).getName());
    }


    @Test
    public void testFetchIntegerTypeArgument() throws Exception {
        engine.add(SpyDefinition.instance().withArguments(1,2,3,4)
                .lookFor(TCLASS1, "paramMethod1"));
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
        engine.add(SpyDefinition.instance().withArguments(1,2)
                .lookFor(TCLASS1, "paramMethod2"));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod2", true, 'A'));
        assertEquals(1, submitter.size());
        assertEquals(true, submitter.get(0).get(0));
        assertEquals('A', submitter.get(0).get(1));
    }


    @Test
    public void testFetchFloatingPointArgs() throws Exception {
        engine.add(SpyDefinition.instance().withArguments(1,2)
                .lookFor(TCLASS1, "paramMethod3"));
        //engine.enableDebug();
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod3", 1.23, (float) 2.34));
        assertEquals(1, submitter.size());
        assertEquals(1.23, (Double)(submitter.get(0).get(0)), 0.01);
        assertEquals(2.34, (Float)(submitter.get(0).get(1)), 0.01);
    }


    @Test
    public void testCheckImmediateFlagInEntryPointOnlyProbe() throws Exception {
        engine.add(SpyDefinition.instance().withTime().lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertEquals(SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testCheckNoFlagOnEnterAndFlushFlagOnExit() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);
    }


    @Test
    public void testCheckNoProbeOnEnterAndImmediateFlagOnExit() throws Exception {
        engine.add(SpyDefinition.instance().onReturn().withTime().lookFor(TCLASS1, "trivialMethod"));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals(1, submitter.size());
        assertEquals(SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testNoProbeOnExitButProbeOnErrorAndOnEnter() throws Exception {
        engine.add(SpyDefinition.instance().withTime().onError().withTime().lookFor(TCLASS1, "trivialMethod"));
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
        engine.add(SpyDefinition.instance().withTime().onReturn().withTime().lookFor(TCLASS1, "errorMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);
        assertTrue("should pass no values", submitter.get(1).nullVals());
    }


    @Test
    public void testIfContextIsTheSameForTheSameClassLoadedTwice() throws Exception {
        engine.add(SpyDefinition.instance().withTime().lookFor(TCLASS1, "trivialMethod"));

        Object obj1 = instantiate(engine, TCLASS1);
        Object obj2 = instantiate(engine, TCLASS1);

        checkForError(invoke(obj1, "trivialMethod"));
        checkForError(invoke(obj2, "trivialMethod"));

        assertEquals(2, submitter.size());
        assertEquals("context IDs should be the same", submitter.get(0).id, submitter.get(1).id);
    }


    @Test
    public void testSubmissionOrderWhenMoreThanOneProbeInOneMethodRet() throws Exception {
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "trivialMethod"));
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "trivialMethod"));

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
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "errorMethod"));
        engine.add(SpyDefinition.instrument().lookFor(TCLASS1, "errorMethod"));

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
        engine.add(SpyDefinition.instance().withArguments(1,2,3).lookFor(TCLASS1, "paramMethod4"));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod4", new int[]{1,2,3}, new byte[]{5,6,7}, new double[]{7,8,9}));

        assertEquals(1, submitter.size());
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(0) instanceof int[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(1) instanceof byte[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(2) instanceof double[]);
    }


    @Test
    public void testFetchReturnObject() throws Exception {
        engine.add(SpyDefinition.instance().onReturn().withArguments(FETCH_RETVAL).lookFor(TCLASS1, "strMethod"));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "strMethod"));

        assertEquals(1, submitter.size());
        assertEquals("oja!", submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnSimpleTypeObject() throws Exception {
        engine.add(SpyDefinition.instance().onReturn().withArguments(FETCH_RETVAL)
                .lookFor(TCLASS1, "getUltimateQuestionOfLife"));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionOfLife"));

        assertEquals(1, submitter.size());
        assertEquals(42, submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnValWithDirtyVariableStack() throws Exception {
        engine.add(SpyDefinition.instance().onReturn().withArguments(FETCH_RETVAL)
                .lookFor(TCLASS1, "getUltimateQuestionWithLocalVars"));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionWithLocalVars"));

        assertEquals(1, submitter.size());
        assertEquals(42, submitter.get(0).get(0));
        assertEquals(retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchCurrentThreadSubmission() throws Exception {
        engine.add(SpyDefinition.instance().withThread().lookFor(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(1, submitter.size());
        assertEquals(Thread.currentThread(), submitter.get(0).get(0));
    }


    @Test
    public void testFetchExceptionObject() throws Exception {
        engine.add(SpyDefinition.instance().onError().withArguments(FETCH_ERROR).lookFor(TCLASS1, "errorMethod"));

        Object obj = instantiate(engine, TCLASS1);
        Object err = invoke(obj, "errorMethod");

        // Check what instrumentation catched
        assertEquals(1, submitter.size());
        assertEquals(err, submitter.get(0).get(0));
        assertTrue("Should return an exception.", submitter.get(0).get(0) instanceof NullPointerException);
        assertEquals("dUP!", ((Exception)submitter.get(0).get(0)).getMessage());
    }

    // TODO test if stack traces in exceptions are the same with and without intercepting errors by instrumentation

    // TODO test instrumenting static method

    // TODO test instrumenting static constructor

    // TODO check if stack traces for instrumented and non-instrumented method are the same if method throws an exception

}
