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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.spy.SpyDefinition;
import com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.jitlogic.zorka.core.test.support.TestUtil.getField;
import static com.jitlogic.zorka.core.test.support.TestUtil.instantiate;
import static com.jitlogic.zorka.core.test.support.TestUtil.invoke;
import static org.junit.Assert.assertEquals;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.field;

public class TracerInstrumentationUnitTest extends BytecodeInstrumentationFixture {


    @Test
    public void testTraceSingleTrivialMethod() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, traceBuilder.size());
    }


    @Test
    public void testInstrumentMethodForTraceAndCheckIfSpyContextHasBeenCreated() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(spy.instance().include(spy.byMethodAnnotation(TCLASS1, "com.SomeTestAnnotation")));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertThat(field("ctxInstances").ofType(Map.class).in(engine).get().size()).isEqualTo(0);
        assertThat(field("ctxById").ofType(Map.class).in(engine).get().size()).isEqualTo(0);
    }


    @Test
    public void testTraceAndInstrumentSingleTrivialMethod() throws Exception {
        engine.add(spy.instance("x").onEnter(spy.fetchArg("E0", 0))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, traceBuilder.size());
        assertEquals(1, submitter.size());
        assertEquals("trivialMethod", symbols.symbolName((Integer) traceBuilder.getData().get(0).get("methodId")));
    }


    @Test
    public void testTraceAndInstrumentRecursiveMethods() throws Exception {
        tracer.include(spy.byMethod(TCLASS2, "~^[a-zA-Z_].*"));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "recursiveMethod");

        Assert.assertEquals("Output actions mismatch.",
                Arrays.asList("traceEnter", "traceEnter", "traceReturn", "traceReturn"), traceBuilder.listAttr("action"));
    }


    @Test
    public void testTraceError() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "~^[a-zA-Z_].*"));

        Object obj = instantiate(engine, TCLASS1);
        Object rslt = invoke(obj, "errorMethod");

        assertEquals(2, traceBuilder.getData().size());
        assertEquals(rslt, traceBuilder.getData().get(1).get("exception"));
        assertEquals("errorMethod", symbols.symbolName((Integer) traceBuilder.getData().get(0).get("methodId")));
    }


    @Test
    public void testTryCatchSimpleCatch() throws Exception {
        tracer.include(spy.byMethod(TCLASS3, "tryCatchFinally0"));

        Object obj = instantiate(engine, TCLASS3);
        invoke(obj, "tryCatchFinally0", true);

        assertEquals("Outer try { } block didn't execute.", 1, getField(obj, "calls"));
        assertEquals("Inner catch { } block didn't execute.", 1, getField(obj, "catches"));
    }


    @Test
    public void testTryCatchFinallyWithEmbeddedCatch() throws Exception {
        tracer.include(spy.byMethod(TCLASS3, "tryCatchFinally1"));

        Object obj = instantiate(engine, TCLASS3);
        invoke(obj, "tryCatchFinally1", true);

        assertEquals("Outer try { } block didn't execute.", 1, getField(obj, "calls"));
        assertEquals("Inner catch { } block didn't execute.", 1, getField(obj, "catches"));
        assertEquals("Outer finally { } block didn't execute.", 1, getField(obj, "finals"));
    }


    @Test
    public void testTryCatchEmbeddedCatch() throws Exception {
        tracer.include(spy.byMethod(TCLASS3, "tryCatchFinally2"));

        Object obj = instantiate(engine, TCLASS3);
        invoke(obj, "tryCatchFinally2", true);

        assertEquals("Outer try { } block didn't execute.", 1, getField(obj, "calls"));
        assertEquals("Inner catch { } block didn't execute.", 1, getField(obj, "catches"));
        assertEquals("Outer finally { } block didn't execute.", 0, getField(obj, "finals"));
    }

    @Test
    public void testIfTracerCatchesSpuriousMethodsBUG() throws Exception {
        tracer.include(spy.byMethod(MCLASS1, "trivia*"));
        tracer.exclude(spy.byClass(MCLASS0));
        tracer.include(spy.byClass("**"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "errorMethod");

        assertEquals(0, traceBuilder.getData().size());

        invoke(obj, "trivialMethod");
        assertEquals(2, traceBuilder.getData().size());
    }
}
