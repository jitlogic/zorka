package com.jitlogic.zorka.core.test.tracer;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.collector.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.output.LTraceHttpOutput;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.TCLASS1;
import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.TCLASS4;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.instantiate;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.invoke;
import static org.junit.Assert.*;

public class LTracerCollectUnitTest extends ZorkaFixture {

    private SymbolRegistry collectorRegistry;
    private MemoryChunkStore collectorStore;
    private Collector collector;
    private ZorkaSubmitter<SymbolicRecord> output;

    private Map<String,String> HTTP_CONF = ZorkaUtil.map(
        "http.qlen", "0",
        "http.retries", "1",
        "http.retry.time", "1",
        "http.retry.exp", "1",
        "http.timeout", "1000"
    );

    @Before
    public void initOutput() {
        collectorRegistry = new SymbolRegistry();
        collectorStore = new MemoryChunkStore();
        collector = new Collector(1, collectorRegistry, collectorStore, false);
        output = new LTraceHttpOutput(config, HTTP_CONF, symbols, new CollectorLocalClient(collector));
    }

    @Test
    public void testSimpleTooShortTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
            spy.instance("1").onEnter(tracer.begin("TEST"))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals("should return begin, trace", 0, collectorStore.size());
    }


    @Test
    public void testSimpleTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));

        spy.add(
            spy.instance("1").onEnter(tracer.begin("TEST", 0))
                .include(spy.byMethod(TCLASS1, "trivialMethod")));

        TraceHandler.setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(1, collector.getAgdCount());
        assertEquals(1, collector.getTrcCount());
        assertEquals(1, collectorStore.size());

        TraceChunkData cd = collectorStore.get(0);
        assertNotNull(cd.getTraceData());
        assertTrue(cd.getTraceData().length > 0);

        assertEquals("TEST", cd.getAttr("component"));
        assertNotNull(cd.getAttr("thread.name"));

        assertNotNull(cd.getMethods());
        assertEquals(cd.getMethods().size(), 1);

        assertNotEquals(cd.getTraceId1(), 0);
        assertNotEquals(cd.getTraceId2(), 0);
        assertNotEquals(cd.getSpanId(), 0);
        assertEquals(cd.getParentId(), 0L);

        TraceDataExtractor extractor = new TraceDataExtractor(collectorRegistry);
        TraceDataResult tr = extractor.extract(Collections.singletonList(cd));
        assertNotNull(tr);
        assertNotEquals("<?>", tr.getMethod());
        assertEquals(TCLASS1 + ".trivialMethod()", tr.getMethod());
        assertEquals("TEST", tr.getTraceType());
        assertEquals("TEST", tr.getAttr("component"));
        assertNotNull(tr.getAttr("thread.name"));
        assertNotNull(tr.getAttr("thread.id"));
        assertNotEquals(tr.getSpanId(), 0);
    }

    @Test
    public void testSimpleTraceWithName() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialStrMethod"));

        spy.add(
            spy.instance("1")
                .onEnter(spy.fetchArg("TAG", 1), tracer.begin("${TAG}", 0))
                .include(spy.byMethod(TCLASS1, "trivialStrMethod")));

        TraceHandler.setMinMethodTime(0); // Catch everything
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialStrMethod", "BUMBUM");

        assertEquals(1, collector.getAgdCount());
        assertEquals(1, collector.getTrcCount());
        assertEquals(1, collectorStore.size());

        TraceChunkData cd = collectorStore.get(0);
        assertNotNull(cd.getTraceData());
        assertTrue(cd.getTraceData().length > 0);

        TraceDataExtractor extractor = new TraceDataExtractor(collectorRegistry);
        TraceDataResult tr = extractor.extract(Collections.singletonList(cd));
        assertNotNull(tr);

        assertEquals(TCLASS1 + ".trivialStrMethod()", tr.getMethod());
        assertEquals("BUMBUM", tr.getTraceType());
        assertEquals(1, tr.getCalls());
    }

    @Test
    public void testSimpleTraceWithAttr() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(spy.instance("1").onEnter(
            tracer.begin("TEST", 0),
            spy.put("URL", "http://some.url"),
            tracer.attr("URL", "URL")
        ).include(spy.byMethod(TCLASS1, "trivialMethod")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertEquals(1, collectorStore.size());
        TraceChunkData cd = collectorStore.get(0);
        assertNotNull(cd.getTraceData());
        assertTrue(cd.getTraceData().length > 0);

        assertEquals("http://some.url", cd.getAttr("URL"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToNamedTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0)).include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance("2").onEnter(tracer.begin("TEST2", 0)).include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance("3").onEnter(tracer.formatTraceAttr("TEST1", "X", "XXX")).include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two embeded traces and top trace", 3, collectorStore.size());

        TraceChunkData cd = collectorStore.get(2);
        assertEquals("TEST1", cd.getAttr("component"));
        assertEquals("XXX", cd.getAttr("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToAnyTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance("2").onEnter(tracer.begin("TEST2", 0))
            .include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance("3").onEnter(tracer.formatTraceAttr(null, "X", "XXX"))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return two embeded traces and top trace", 3, collectorStore.size());
        assertEquals("TEST2", collectorStore.get(1).getAttr("component"));
        assertEquals("XXX", collectorStore.get(1).getAttr("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToUnknownTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance("2").onEnter(tracer.begin("TEST2", 0))
            .include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance("3").onEnter(tracer.formatTraceAttr("TEST3", "X", "XXX"))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals(3, collectorStore.size());
        assertNull(collectorStore.get(0).getAttr("X"));
        assertNull(collectorStore.get(1).getAttr("X"));
        assertNull(collectorStore.get(2).getAttr("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodNamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0),
            tracer.formatTraceAttr("TEST1", "X", "XXX"))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals(1, collectorStore.size());
        assertEquals("TEST1", collectorStore.get(0).getAttr("component"));
        assertEquals("XXX", collectorStore.get(0).getAttr("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodUnnamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1")
            .onEnter(tracer.begin("TEST1", 0),
                tracer.formatTraceAttr(null, "X", "XXX"))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals(1, collectorStore.size());
        assertEquals("TEST1", collectorStore.get(0).getAttr("component"));
        assertEquals("XXX", collectorStore.get(0).getAttr("X"));
    }

    @Test
    public void testTraceFlagsUpwardPropagation() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance("2").onEnter(tracer.begin("TEST2", 0))
            .include(spy.byMethod(TCLASS4, "recursive2")));

        spy.add(spy.instance("3")
            .onEnter(tracer.traceFlags("TEST1", TraceMarker.ERROR_MARK))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals(3, collectorStore.size());

        // TODO assertTrue("Error flag should be enabled for TEST1 trace", collectorStore.get(2).hasError());
        // TODO assertFalse("Error flag should be disabled for TEST2 trace", collectorStore.get(1).hasError());
    }

    @Test
    public void testInTraceCheckerPositiveCheck() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance("2")
            .onEnter(spy.subchain(tracer.inTrace("TEST1"),
                tracer.formatTraceAttr("TEST1", "IN", "YES")))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, collectorStore.size());

        assertEquals("YES", collectorStore.get(0).getAttr("IN"));
    }

    @Test
    public void testInTraceCheckerNegativeCheck() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance("2")
            .onEnter(spy.subchain(tracer.inTrace("TEST2"),
                tracer.formatTraceAttr("TEST1", "IN", "YES")))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, collectorStore.size());

        assertNull(collectorStore.get(0).getAttr("IN"));
    }

    @Test
    public void testTraceSpyMethodsFlagOn() throws Exception {
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance("2")
            .onEnter(spy.put("AA", "OJA"))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(true);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, collectorStore.size());
        // TODO assertEquals("trace should also register recursive1 method", 1, results.get(0).numChildren());
    }

    @Test
    public void testTraceSpyMethodsFlagOff() throws Exception {
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance("2")
            .onEnter(spy.put("AA", "OJA"))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(false);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return no trace", 0, collectorStore.size());
    }

    @Test
    public void testTraceSpyMethodsFlagOffAndOneMethodManuallyAddedToTracer() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recursive3"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
            .include(spy.byMethod(TCLASS4, "recursive3")));

        spy.add(spy.instance("2")
            .onEnter(spy.put("AA", "OJA"))
            .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);

        agentInstance.getTracer().setTraceSpyMethods(false);

        tracer.output(output);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        assertEquals("should return one trace", 1, collectorStore.size());
        // TODO assertEquals("trace should not register recursive1 method", 0, results.get(0).numChildren());
    }


}
