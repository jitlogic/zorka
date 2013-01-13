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

import com.jitlogic.zorka.test.spy.support.TestTracer;
import com.jitlogic.zorka.test.support.ZorkaFixture;

import com.jitlogic.zorka.tracer.SymbolRegistry;
import com.jitlogic.zorka.tracer.Tracer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.test.support.BytecodeInstrumentationFixture.*;
import static com.jitlogic.zorka.test.support.TestUtil.*;

public class TracerIntegTest extends ZorkaFixture {

    private TestTracer output = new TestTracer();

    private int sym(String s) {
        return spyInstance.getTracer().getSymbolRegistry().symbolId(s);
    }


    @Test
    public void testSimpleTooShortTrace() throws Exception {
        spy.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
                spy.instance().onEnter(spy.traceBegin("TEST"))
                        .include(spy.byMethod(TCLASS1, "trivialMethod")));
        spy.add(output);

        Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return traceBegin, trace", 0, output.size());
    }


    @Test
    public void testSimpleTrace() throws Exception {
        spy.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
            spy.instance().onEnter(spy.traceBegin("TEST"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        spyInstance.getTracer().setMethodTime(0); // Catch everything
        spy.add(output);

        Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return traceBegin, trace", 4, output.size());
        output.check(0, "action", "traceBegin", "traceId", sym("TEST"));
        output.check(1, "action", "traceEnter", "classId", sym(TCLASS1), "methodId", sym("trivialMethod"));
        output.check(2, "action", "traceStats", "calls", 1L, "errors", 0L);
        output.check(3, "action", "traceReturn");
    }


    @Test
    public void testSimpleTraceWithAttr() throws Exception {
        spy.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(spy.instance().onEnter(
                spy.traceBegin("TEST"), spy.put("URL", "http://some.url"), spy.traceAttr("URL", "URL")
            ).include(spy.byMethod(TCLASS1, "trivialMethod")));
        spyInstance.getTracer().setMethodTime(0); // Catch everything
        spy.add(output);

        Object obj = instantiate(spyInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return traceBegin, trace", 5, output.size());
        output.check(0, "action", "traceBegin", "traceId", sym("TEST"));
        output.check(1, "action", "traceEnter", "classId", sym(TCLASS1), "methodId", sym("trivialMethod"));
        output.check(2, "action", "traceStats", "calls", 1L, "errors", 0L);
        output.check(3, "action", "newAttr", "attrId", sym("URL"), "attrVal", "http://some.url");
        output.check(4, "action", "traceReturn");
    }

}
