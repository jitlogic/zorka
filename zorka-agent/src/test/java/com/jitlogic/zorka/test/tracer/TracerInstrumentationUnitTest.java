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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.test.tracer;

import com.jitlogic.zorka.spy.SpyDefinition;
import com.jitlogic.zorka.test.spy.support.TestSpyTransformer;
import com.jitlogic.zorka.test.spy.support.TestSubmitter;
import com.jitlogic.zorka.test.spy.support.TestTracer;
import com.jitlogic.zorka.test.support.BytecodeInstrumentationFixture;
import com.jitlogic.zorka.tracer.SymbolRegistry;
import com.jitlogic.zorka.tracer.WrappedException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static com.jitlogic.zorka.test.support.TestUtil.instantiate;
import static com.jitlogic.zorka.test.support.TestUtil.invoke;
import static org.junit.Assert.assertEquals;

public class TracerInstrumentationUnitTest extends BytecodeInstrumentationFixture {


    @Test
    public void testTraceSingleTrivialMethod() throws Exception {
        engine.add(spy.byMethod(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, output.size());
    }


    @Test
    public void testTraceAndInstrumentSingleTrivialMethod() throws Exception {
        engine.add(SpyDefinition.instance().onEnter(spy.fetchArg("E0", 0))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        engine.add(spy.byMethod(TCLASS1, "trivialMethod"));

        Object obj = instantiate(engine, TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(2, output.size());
        assertEquals(1, submitter.size());
        assertEquals("trivialMethod", symbols.symbolName((Integer)output.getData().get(0).get("methodId")));
    }


    @Test
    public void testTraceAndInstrumentRecursiveMethods() throws Exception {
        engine.add(spy.byMethod(TCLASS2, "~^[a-zA-Z_].*"));

        Object obj = instantiate(engine, TCLASS2);
        invoke(obj, "recursiveMethod");

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceEnter", "traceEnter", "traceReturn", "traceReturn"), output.listAttr("action"));
    }


    @Test
    public void testTraceError() throws Exception {
        engine.add(spy.byMethod(TCLASS1, "~^[a-zA-Z_].*"));

        Object obj = instantiate(engine, TCLASS1);
        Object rslt = invoke(obj, "errorMethod");

        assertEquals(2, output.getData().size());
        assertEquals(new WrappedException((Throwable)rslt), output.getData().get(1).get("exception"));
        assertEquals("errorMethod", symbols.symbolName((Integer)output.getData().get(0).get("methodId")));
    }


}
