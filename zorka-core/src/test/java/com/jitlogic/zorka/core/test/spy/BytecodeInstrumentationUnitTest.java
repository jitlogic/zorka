/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.core.test.spy.support.TestCollector;
import com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture;

import org.junit.Test;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.core.spy.SpyLib.*;

import static com.jitlogic.zorka.core.test.support.TestUtil.*;

public class BytecodeInstrumentationUnitTest extends BytecodeInstrumentationFixture {



    @Test
    public void testClassWithoutAnyTransform() throws Exception {
        Object obj = instantiate(engine, TCLASS1);
        assertNotNull("should instantiate properly", obj);
        assertEquals("should be of class " + TCLASS1, TCLASS1, obj.getClass().getName());
    }


    @Test
    public void testTrivialInstrumentOnlyEntryPointWithThisRef() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 0))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should submit once", 1, submitter.size());
        assertEquals("probe should return reference to itself", obj, submitter.get(0).get(0));
    }


    @Test
    public void testStaticNotPublicMethod() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchTime("R0"))
                .include(spy.byMethod(0, TCLASS1, "nonPublicStatic", null)));

        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "nonPublicStatic");

        assertEquals(1, getField(obj, "scalls"));

        assertEquals("should submit one record", 1, submitter.size());
    }


    @Test
    public void testInstrumentWithTimeProbe() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should return one object", 1, submitter.size());
        assertTrue("should return value of type Long", submitter.get(0).get(0) instanceof Long);
    }


    @Test
    public void testTrivialInstrumentOnlyEntryPointWithCurrentTime() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch entry point", 1, submitter.size());
        assertTrue("should return Long", submitter.get(0).get(0) instanceof Long);

    }


    @Test
    public void testInstrumentWithTimeOnEnterExit() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals("should catch both entry and exit points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTimeOnEnterError() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "errorMethod")));
        Object obj = instantiate(engine, TCLASS1);

        invoke(obj, "errorMethod");

        assertEquals("should catch both entry and error points", 2, submitter.size());
        assertTrue("should pass Long", submitter.get(0).get(0) instanceof Long);
        assertTrue("should pass Long", submitter.get(1).get(0) instanceof Long);
    }


    @Test
    public void testInstrumentWithTwoProbes() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "trivialMethod")));
        engine.add(spy.instance("y").onEnter(spy.fetchArg("E0", 0))
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
        engine.add(spy.instrument("y").include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));
        instantiate(engine, TCLASS1);

        assertEquals("should submit two records", 2, submitter.size());
    }


    @Test
    public void testInstrumentConstructorWithSelfRef() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchArg("R0", 0))
                .include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));
        Object obj = instantiate(engine, TCLASS1);

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should return object itself", obj, submitter.get(0).get(0));
    }


    @Test
    public void testInstrumentConstructorWithInvalidSelfRefOnBeginning() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 0))
                .include(spy.byMethod(TCLASS1, SM_CONSTRUCTOR)));

        instantiate(engine, TCLASS1);

        assertEquals("should submit one record", 1, submitter.size());
        assertNull("should return null instead of ", submitter.get(0).get(0));
    }


    @Test
    public void testFetchClassFromInstrumentedCode() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchClass("E0", TCLASS1))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit one record", 1, submitter.size());
        assertTrue("should fetch class object", submitter.get(0).get(0) instanceof Class);
        assertEquals("class should be of " + TCLASS1, TCLASS1, ((Class) (submitter.get(0).get(0))).getName());
    }


    @Test
    public void testFetchIntegerTypeArgument() throws Exception {
        engine.add(spy.instance("x")
                .onEnter(spy.fetchArg("E0", 1), spy.fetchArg("E1", 2), spy.fetchArg("E2", 3), spy.fetchArg("E3", 4))
                .include(spy.byMethod(TCLASS1, "paramMethod1")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod1", 10, 20L, (short) 30, (byte) 40));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should fetch integer as first parameter", Integer.valueOf(10), submitter.get(0).get(0));
        assertEquals("should fetch long as second parameter", Long.valueOf(20), submitter.get(0).get(1));
        assertEquals("should fetch short as third parameter", (short) 30, submitter.get(0).get(2));
        assertEquals("should fetch byte as fourth parameter", (byte) 40, submitter.get(0).get(3));
    }


    @Test
    public void testFetchBooleanCharTypeArgument() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 1), spy.fetchArg("E1", 2))
                .include(spy.byMethod(TCLASS1, "paramMethod2")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod2", true, 'A'));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should submit boolean as first parameter", true, submitter.get(0).get(0));
        assertEquals("should submit character as second parameter", 'A', submitter.get(0).get(1));
    }


    @Test
    public void testFetchFloatingPointArgs() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 1), spy.fetchArg("E1", 2))
                .include(spy.byMethod(TCLASS1, "paramMethod3")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod3", 1.23, (float) 2.34));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should fetch double as first parameter", 1.23, (Double) (submitter.get(0).get(0)), 0.01);
        assertEquals("should fetch float as second parameter", 2.34, (Float) (submitter.get(0).get(1)), 0.01);
    }


    @Test
    public void testCheckImmediateFlagInEntryPointOnlyProbe() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("record should be submitted with IMMEDIATE flag", SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testCheckNoFlagOnEnterAndFlushFlagOnExit() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit two records", 2, submitter.size());
        assertEquals("first record should carry no flags", SF_NONE, submitter.get(0).submitFlags);
        assertEquals("second record should carry FLUSH flag", SF_FLUSH, submitter.get(1).submitFlags);
    }


    @Test
    public void testCheckNoProbeOnEnterAndImmediateFlagOnExit() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchTime("R0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("record should carry IMMEDIATE flag", SF_IMMEDIATE, submitter.get(0).submitFlags);
    }


    @Test
    public void testNoProbeOnExitButProbeOnErrorAndOnEnter() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0")).onError(spy.fetchTime("X0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit two records", 2, submitter.size());
        assertEquals("first record should carry no flags", SF_NONE, submitter.get(0).submitFlags);
        assertEquals("second record should carry FLUSH flag", SF_FLUSH, submitter.get(1).submitFlags);
        assertTrue("should pass no values", submitter.get(1).nullVals());
    }


    @Test
    public void testNoProbeOnErrorButProbeOnEnterAndExit() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0")).onReturn(spy.fetchTime("X0"))
                .include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals("should submit two records", 2, submitter.size());
        assertEquals("first record should carry no flags", SF_NONE, submitter.get(0).submitFlags);
        assertEquals("second record should carry FLUSH flag", SF_FLUSH, submitter.get(1).submitFlags);
        assertTrue("should pass no values", submitter.get(1).nullVals());
    }


    @Test
    public void testIfContextIsTheSameForTheSameClassLoadedTwice() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj1 = instantiate(engine, TCLASS1);
        Object obj2 = instantiate(engine, TCLASS1);

        checkForError(invoke(obj1, "trivialMethod"));
        checkForError(invoke(obj2, "trivialMethod"));

        assertEquals("should submit two records", 2, submitter.size());
        assertEquals("context IDs should be the same", submitter.get(0).id, submitter.get(1).id);
    }


    @Test
    public void testSubmissionOrderWhenMoreThanOneProbeInOneMethodRet() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "trivialMethod")));
        engine.add(spy.instrument("y").include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit 4 records", 4, submitter.size());

        assertEquals("first and last submit should have the same context ID:",
                submitter.get(0).id, submitter.get(3).id);

        assertEquals("second and third submit should have the same context ID:",
                submitter.get(1).id, submitter.get(2).id);
    }


    @Test
    public void testSubmissionOrderWhenMoreThanOneProbeInOneMethodErr() throws Exception {
        engine.add(spy.instrument("x").include(spy.byMethod(TCLASS1, "errorMethod")));
        engine.add(spy.instrument("y").include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals("should submit 4 records", 4, submitter.size());

        assertEquals("first and last submit should have the same context ID:",
                submitter.get(0).id, submitter.get(3).id);

        assertEquals("second and third submit should have the same context ID:",
                submitter.get(1).id, submitter.get(2).id);
    }


    @Test
    public void testFetchArraysOfSimpleTypes() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 1), spy.fetchArg("E1", 2), spy.fetchArg("E2", 3))
                .include(spy.byMethod(TCLASS1, "paramMethod4")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "paramMethod4", new int[]{1, 2, 3}, new byte[]{5, 6, 7}, new double[]{7, 8, 9}));

        assertEquals("should submit one record", 1, submitter.size());
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(0) instanceof int[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(1) instanceof byte[]);
        assertTrue("first parameter should be an array of integers", submitter.get(0).get(2) instanceof double[]);
    }


    @Test
    public void testFetchReturnObject() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "strMethod")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "strMethod"));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should catch string return value", "oja!", submitter.get(0).get(0));
        assertEquals("fetched value should be the same as returned", retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnSimpleTypeObject() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "getUltimateQuestionOfLife")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionOfLife"));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should return integer value", 42, submitter.get(0).get(0));
        assertEquals("fetched value should be the same as returned", retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchReturnValWithDirtyVariableStack() throws Exception {
        engine.add(spy.instance("x").onReturn(spy.fetchRetVal("R0"))
                .include(spy.byMethod(TCLASS1, "getUltimateQuestionWithLocalVars")));

        Object obj = instantiate(engine, TCLASS1);

        Object retVal = checkForError(invoke(obj, "getUltimateQuestionWithLocalVars"));

        assertEquals("should return one record", 1, submitter.size());
        assertEquals("should return integer value", 42, submitter.get(0).get(0));
        assertEquals("fetched value should be the same as returned", retVal, submitter.get(0).get(0));
    }


    @Test
    public void testFetchCurrentThreadSubmission() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchThread("E0"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("should return current thread object", Thread.currentThread(), submitter.get(0).get(0));
    }


    @Test
    public void testFetchExceptionObject() throws Exception {
        engine.add(spy.instance("x").onError(spy.fetchError("X0"))
                .include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        Object err = invoke(obj, "errorMethod");

        // Check what instrumentation catched
        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("fetched value should be the same as returned", err, submitter.get(0).get(0));
        assertTrue("Should return an exception.", submitter.get(0).get(0) instanceof NullPointerException);
        assertEquals("dUP!", ((Exception) submitter.get(0).get(0)).getMessage());
    }


    @Test
    public void testInstrumentClassByAnnotation() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("X"))
                .include(spy.byClassAnnotation(TACLASS)));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, submitter.size());
    }


    @Test
    public void testNonInstrumentClassByAnnotation() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("X"))
                .include(spy.byClassAnnotation(TACLASS)));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "trivialMethod");

        assertEquals("should submit no records", 0, submitter.size());
    }


    @Test
    public void testMatchClassByMethodAnnotation() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("X"))
                .include(spy.byMethodAnnotation(TCLASS2, TAMETHOD)));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "trivialMethod");

        assertEquals("should submit one record", 1, submitter.size());
    }


    @Test
    public void testNonMatchClassByMethodAnnotation() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("X"))
                .include(spy.byMethodAnnotation(TCLASS2, TAMETHOD)));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should submit no records", 0, submitter.size());
    }


    @Test
    public void testInstrumentClassWithNoStack() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("T1")).onReturn(spy.fetchTime("T2"))
                .include(spy.byMethod(TCLASS2, "echoInt")));

        Object obj = instantiate(engine, TCLASS2);
        checkForError(invoke(obj, "echoInt", 10));

        assertEquals("should submit two records", 2, submitter.size());
    }


    @Test
    public void testInstrumentMethodEntryWithoutProbes() throws Exception {
        SpyProcessor col = new TestCollector();
        engine.add(spy.instance("x").onEnter(col).include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit one record", 1, submitter.size());
    }


    @Test
    public void testInstrumentMethodReturnWithoutProbes() throws Exception {
        SpyProcessor col = new TestCollector();
        engine.add(spy.instance("x").onReturn(col).include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        checkForError(invoke(obj, "trivialMethod"));

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("record should carry no values", 0, submitter.get(0).size());
    }


    @Test
    public void testInstrumentMethodErrorWithoutProbes() throws Exception {
        SpyProcessor col = new TestCollector();
        engine.add(spy.instance("x").onError(col).include(spy.byMethod(TCLASS1, "errorMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals("should submit one record", 1, submitter.size());
        assertEquals("record should carry no values", 0, submitter.get(0).size());
    }


    @Test
    public void testInstrumentWithAssymetricProbePlacements() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchTime("ENTER"))
                .onReturn(spy.put("RETURN", 1)).onError(spy.put("ERROR", 1))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, submitter.size());
        assertEquals(SF_NONE, submitter.get(0).submitFlags);
        assertEquals(SF_FLUSH, submitter.get(1).submitFlags);

    }


    @Test
    public void testInstrumentDirectInterfaceMethod() throws Exception {
        engine.add(spy.instrument("x").include(spy.byInterfaceAndMethod(ICLASS1, "myMethod1")));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "myMethod1");

        assertEquals(2, submitter.size());
    }


    @Test
    public void testInstrumentNonRecursiveIndirectInterfaceMethod() throws Exception {
        engine.add(spy.instrument("x").include(spy.byInterfaceAndMethod(ICLASS2, "myMethod2")));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "myMethod2");

        assertEquals("spy should not match anything", 0, submitter.size());
    }


    @Test
    public void testInstrumentRecursiveIndirectInterfaceMethod() throws Exception {
        engine.add(spy.instrument("x").include(
                spy.byInterfaceAndMethod(ICLASS2, "myMethod2").recursive()));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "myMethod2");

        assertEquals("spy match", 2, submitter.size());
    }


    @Test
    public void testInstrumentRecursiveIndirectByClassInterfaceMethod() throws Exception {
        engine.add(spy.instrument("x").include(
                spy.byInterfaceAndMethod(ICLASS1, "trivialMethod4").recursive()));

        Object obj = instantiate(engine, TCLASS4);
        invoke(obj, "trivialMethod4");

        assertEquals(2, submitter.size());
    }

    @Test
    public void testInstrumentComplicatedMethodWithVariable() throws Exception {

        engine.add(spy.instrument("x").include(
                spy.byMethod(TCLASS1, "complicatedMethod")));

        Object obj = instantiate(engine, TCLASS1);
        Object rslt = invoke(obj, "complicatedMethod", "123");
        assertEquals(Integer.valueOf("123"), rslt);

        assertEquals(2, submitter.size());
    }

    // TODO test if stack traces in exceptions are the same with and without intercepting errors by instrumentation

    // TODO test instrumenting static method

    // TODO test instrumenting static constructor

    // TODO check if stack traces for instrumented and non-instrumented method are the same if method throws an exception


    // TODO check if ID of method with the same name is the same for two different classes
}
