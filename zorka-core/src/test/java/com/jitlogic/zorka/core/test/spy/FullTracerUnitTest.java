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

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.*;
import static com.jitlogic.zorka.core.test.support.TestUtil.*;

public class FullTracerUnitTest extends ZorkaFixture {

    private List<TraceRecord> results = new ArrayList<TraceRecord>();

    private ZorkaSubmitter<SymbolicRecord> output;

    private int sym(String s) {
        return agentInstance.getSymbolRegistry().symbolId(s);
    }


    @Before
    public void initOutput() {
        output = new ZorkaSubmitter<SymbolicRecord>() {
            @Override
            public boolean submit(SymbolicRecord obj) {
                return results.add((TraceRecord) obj);
            }
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

        assertEquals("should return begin, trace", 0, results.size());
    }


    @Test
    public void testSimpleTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));

        spy.add(
                spy.instance().onEnter(tracer.begin("TEST", 0))
                        .include(spy.byMethod(TCLASS1, "trivialMethod")));

        agentInstance.getTracer().setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 1, results.size());
        TraceRecord rec = results.get(0);
        assertEquals(sym(TCLASS1), rec.getClassId());
        assertEquals(sym("TEST"), rec.getTraceId());
        assertEquals(1, rec.getCalls());
    }


    @Test
    public void testSimpleTraceWithName() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialStrMethod"));

        spy.add(
          spy.instance()
            .onEnter(spy.fetchArg("TAG", 1), tracer.begin("${TAG}", 0))
            .include(spy.byMethod(TCLASS1, "trivialStrMethod")));

        agentInstance.getTracer().setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialStrMethod", "BUMBUM");
        assertEquals("should return begin, trace", 1, results.size());

        TraceRecord rec = results.get(0);
        assertEquals(sym(TCLASS1), rec.getClassId());
        assertEquals(sym("BUMBUM"), rec.getTraceId());
        assertEquals(1, rec.getCalls());
    }


    @Test
    public void testSimpleTraceWithAttr() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(spy.instance().onEnter(
                tracer.begin("TEST", 0),
                spy.put("URL", "http://some.url"),
                tracer.attr("URL", "URL")
        ).include(spy.byMethod(TCLASS1, "trivialMethod")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 1, results.size());
        assertNotNull(results.get(0).getAttrs());
        assertEquals("http://some.url", results.get(0).getAttr(symbols.symbolId("URL")));
    }


    @Test
    public void testTraceAttrUpwardPropagationToNamedTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance().onEnter(tracer.begin("TEST2", 0)).include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance().onEnter(tracer.formatTraceAttr("TEST1", "X", "XXX")).include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 2, results.size());
        assertEquals("TEST1", symbols.symbolName(results.get(1).getMarker().getTraceId()));
        assertEquals("XXX", results.get(1).getAttr(symbols.symbolId("X")));
    }


    @Test
    public void testTraceAttrUpwardPropagationToAnyTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance().onEnter(tracer.begin("TEST2", 0)).include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance().onEnter(tracer.formatTraceAttr(null, "X", "XXX")).include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 2, results.size());
        assertEquals("TEST2", symbols.symbolName(results.get(0).getMarker().getTraceId()));
        assertEquals("XXX", results.get(0).getAttr(symbols.symbolId("X")));
    }

    @Test
    public void testTraceAttrUpwardPropagationToUnknownTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance().onEnter(tracer.begin("TEST2", 0)).include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance().onEnter(tracer.formatTraceAttr("TEST3", "X", "XXX")).include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 2, results.size());
        assertEquals(null, results.get(0).getAttr(symbols.symbolId("X")));
        assertEquals(null, results.get(1).getAttr(symbols.symbolId("X")));
    }


    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodNamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0), tracer.formatTraceAttr("TEST1", "X", "XXX")).include(spy.byMethod(TCLASS4, "recursive3")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 1, results.size());
        assertEquals("TEST1", symbols.symbolName(results.get(0).getMarker().getTraceId()));
        assertEquals("XXX", results.get(0).getAttr(symbols.symbolId("X")));
    }


    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodUnnamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance()
                .onEnter(tracer.begin("TEST1", 0), tracer.formatTraceAttr(null, "X", "XXX"))
                .include(spy.byMethod(TCLASS4, "recursive3")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 1, results.size());
        assertEquals("TEST1", symbols.symbolName(results.get(0).getMarker().getTraceId()));
        assertEquals("XXX", results.get(0).getAttr(symbols.symbolId("X")));
    }


    @Test
    public void testGetTraceAttrFromUpperAndCurrentFrame() throws Exception {
        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        tracer.include(spy.byMethod(TCLASS4, "recur*"));

        spy.add(spy.instance()
                .onEnter(tracer.begin("TEST1", 0), tracer.formatAttr("FIELD1", "XXX"))
                .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(tracer.begin("TEST2", 0),
                    tracer.formatAttr("FIELD2", "YYY"),
                    tracer.getTraceAttr("FIELD2C", "FIELD2"),
                    tracer.attr("FIELD2C", "FIELD2C"),
                    tracer.getTraceAttr("FIELD1C", "TEST1", "FIELD1"),
                    tracer.attr("FIELD1C", "FIELD1C"))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        // Check if both traces came back
        assertEquals(2, results.size());
        assertEquals("TEST2", symbols.symbolName(results.get(0).getMarker().getTraceId()));
        assertEquals("TEST1", symbols.symbolName(results.get(1).getMarker().getTraceId()));

        // Check standard attributes are set
        assertEquals("YYY", results.get(0).getAttr(symbols.symbolId("FIELD2")));
        assertEquals("XXX", results.get(1).getAttr(symbols.symbolId("FIELD1")));

        // Check if attributes taken from another trace are set
        assertEquals("XXX", results.get(0).getAttr(symbols.symbolId("FIELD1C")));
        assertEquals("YYY", results.get(0).getAttr(symbols.symbolId("FIELD2C")));
    }


    @Test
    public void testTraceFlagsUpwardPropagation() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance().onEnter(tracer.begin("TEST2", 0)).include(spy.byMethod(TCLASS4, "recursive2")));

        spy.add(spy.instance()
                .onEnter(tracer.traceFlags("TEST1", TraceMarker.ERROR_MARK))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two traces", 2, results.size());

        assertTrue("Error flag should be enabled for TEST1 trace",
                results.get(1).getMarker().hasFlag(TraceMarker.ERROR_MARK));

        assertFalse("Error flag should be disabled for TEST2 trace",
                results.get(0).getMarker().hasFlag(TraceMarker.ERROR_MARK));
    }


    @Test
    public void testInTraceCheckerPositiveCheck() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(spy.subchain(tracer.inTrace("TEST1"), tracer.formatTraceAttr("TEST1", "IN", "YES")))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, results.size());

        assertEquals("YES", results.get(0).getAttr(symbols.symbolId("IN")));
    }

    @Test
    public void testInTraceCheckerNegativeCheck() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(spy.subchain(tracer.inTrace("TEST2"), tracer.formatTraceAttr("TEST1", "IN", "YES")))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, results.size());

        assertEquals(null, results.get(0).getAttr(symbols.symbolId("IN")));
    }


    @Test
    public void testTraceSpyMethodsFlagOn() throws Exception {
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(spy.put("AA", "OJA"))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(true);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, results.size());
        assertEquals("trace should also register recursive1 method", 1, results.get(0).numChildren());
    }

    @Test
    public void testTraceSpyMethodsFlagOff() throws Exception {
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(spy.put("AA", "OJA"))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(false);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return no trace", 0, results.size());
    }


    @Test
    public void testTraceSpyMethodsFlagOffAndOneMethodManuallyAddedToTracer() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recursive3"));
        spy.add(spy.instance().onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance()
                .onEnter(spy.put("AA", "OJA"))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        agentInstance.getTracer().setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(false);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, results.size());
        assertEquals("trace should not register recursive1 method", 0, results.get(0).numChildren());
    }

}
