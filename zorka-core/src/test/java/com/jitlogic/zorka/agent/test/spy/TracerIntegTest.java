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

package com.jitlogic.zorka.agent.test.spy;

import com.jitlogic.zorka.agent.test.spy.support.TestTracer;
import com.jitlogic.zorka.agent.test.support.ZorkaFixture;

import com.jitlogic.zorka.agent.util.Submittable;
import com.jitlogic.zorka.agent.util.ZorkaAsyncThread;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.agent.test.support.BytecodeInstrumentationFixture.*;
import static com.jitlogic.zorka.agent.test.support.TestUtil.*;

public class TracerIntegTest extends ZorkaFixture {

    private TestTracer rslt = new TestTracer();

    private ZorkaAsyncThread<Submittable> output;

    private int sym(String s) {
        return agentInstance.getTracer().getSymbolRegistry().symbolId(s);
    }

    @Before
    public void initOutput() {
        rslt = new TestTracer();
        output = new ZorkaAsyncThread<Submittable>("test") {
            @Override public boolean submit(Submittable obj) {
                obj.traverse(rslt);
                return true;
            }
            @Override protected void process(Submittable obj) { }
        };
    }

    @Test
    public void testSimpleTooShortTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
                spy.instance().onEnter(tracer.begin("TEST"))
                        .include(spy.byMethod(TCLASS1, "trivialMethod")));
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 0, rslt.size());
    }


    @Test
    public void testSimpleTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
            spy.instance().onEnter(tracer.begin("TEST"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        agentInstance.getTracer().setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 4, rslt.size());
        rslt.check(0, "action", "begin", "traceId", sym("TEST"));
        rslt.check(1, "action", "traceEnter", "classId", sym(TCLASS1), "methodId", sym("trivialMethod"));
        rslt.check(2, "action", "traceStats", "calls", 1L, "errors", 0L);
        rslt.check(3, "action", "traceReturn");

        assertTrue("clock time should be set to non-zero value", (Long)rslt.get(0, "clock") > 0);
    }


    @Test
    public void testSimpleTraceWithAttr() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(spy.instance().onEnter(
                tracer.begin("TEST"), spy.put("URL", "http://some.url"), tracer.attr("URL", "URL")
        ).include(spy.byMethod(TCLASS1, "trivialMethod")));

        agentInstance.getTracer().setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 5, rslt.size());
        rslt.check(0, "action", "begin", "traceId", sym("TEST"));
        rslt.check(1, "action", "traceEnter", "classId", sym(TCLASS1), "methodId", sym("trivialMethod"));
        rslt.check(2, "action", "traceStats", "calls", 1L, "errors", 0L);
        rslt.check(3, "action", "newAttr", "attrId", sym("URL"), "attrVal", "http://some.url");
        rslt.check(4, "action", "traceReturn");
    }

}
