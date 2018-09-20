package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.Tracer;
import com.jitlogic.zorka.core.spy.lt.TraceHandler;
import com.jitlogic.zorka.core.test.spy.support.cbor.STBeg;
import com.jitlogic.zorka.core.test.spy.support.cbor.STErr;
import com.jitlogic.zorka.core.test.spy.support.cbor.STRec;
import com.jitlogic.zorka.core.test.spy.support.cbor.TestTraceBufOutput;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.jitlogic.zorka.core.test.spy.support.cbor.STTrcTestUtils.chunksCount;
import static com.jitlogic.zorka.core.test.spy.support.cbor.STTrcTestUtils.parseTrace;
import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.TCLASS4;
import static org.junit.Assert.*;

import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.TCLASS1;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.instantiate;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.invoke;

public class STracerFullUnitTest extends ZorkaFixture {

    private TestTraceBufOutput o = new TestTraceBufOutput();

    public STracerFullUnitTest() {
        configProperties.setProperty("tracer.type", "streaming");
    }

    private static Object l(Object...args) {
        return Arrays.asList(args);
    }

    private static Map m(Object...args) {
        return ZorkaUtil.map(args);
    }


    @Test
    public void testSimpleTooShortTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));
        spy.add(
                spy.instance("1").onEnter(tracer.begin("TEST"))
                        .include(spy.byMethod(TCLASS1, "trivialMethod")));
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        assertNull("should return begin, trace", o.getChunks());
    }


    @Test
    public void testSimpleTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialMethod"));

        spy.add(spy.instance("1").onEnter(tracer.begin("TEST", 0))
                        .include(spy.byMethod(TCLASS1, "trivialMethod")));

        TraceHandler.setMinMethodTime(0); // Catch everything

        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);

        invoke(obj, "trivialMethod");

        assertEquals(1, chunksCount(o.getChunks()));

        STRec tr = o.getChunks() != null ? parseTrace(o.getChunks(), symbols) : null;
        assertNotNull(tr);

        assertEquals(TCLASS1, tr.getClassName());
        assertEquals(1, tr.getCalls());

        STBeg tb = tr.getBegin();
        assertNotNull(tb);

        assertEquals("TEST", tb.getTraceName());
    }


    @Test
    public void testSimpleTraceWithName() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "trivialStrMethod"));

        spy.add(
                spy.instance("1")
                        .onEnter(spy.fetchArg("TAG", 1), tracer.begin("${TAG}", 0))
                        .include(spy.byMethod(TCLASS1, "trivialStrMethod")));

        TraceHandler.setMinMethodTime(0); // Catch everything
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialStrMethod", "BUMBUM");
        assertNotNull("should return begin, trace", o.getChunks());

        STRec tr = o.getChunks() != null ? parseTrace(o.getChunks(), symbols) : null;
        assertNotNull(tr);
        assertEquals(TCLASS1, tr.getClassName());
        assertEquals(1, tr.getCalls());

        STBeg tb = tr.getBegin();
        assertNotNull(tb);
        assertEquals("BUMBUM", tr.getBegin().getTraceName());
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
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);
        invoke(obj, "trivialMethod");

        STRec tr = o.getChunks() != null ? parseTrace(o.getChunks(), symbols) : null;
        assertNotNull(tr);
        assertEquals("http://some.url", tr.getAttrs().get("URL"));
    }


    @Test
    public void testTraceAttrUpwardPropagationToNamedTrace() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0))
                .include(spy.byMethod(TCLASS4, "recursive3")));
        spy.add(spy.instance("2").onEnter(tracer.begin("TEST2", 0))
                .include(spy.byMethod(TCLASS4, "recursive2")));
        spy.add(spy.instance("3").onEnter(tracer.formatTraceAttr("TEST1", "X", "XXX"))
                .include(spy.byMethod(TCLASS4, "recursive1")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        STRec tr = parseTrace(o.getChunks(), symbols);

        assertEquals("XXX", tr.getAttrs().get("X"));
        assertEquals("TEST1", tr.getBegin().getTraceName());
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
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        STRec tr = parseTrace(o.getChunks(), symbols);
        STRec t1 = tr.getChildren().get(0);

        assertEquals("TEST2", t1.getBegin().getTraceName());
        assertEquals("XXX", t1.getAttrs().get("X"));
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
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        STRec tr = parseTrace(o.getChunks(), symbols);
        STRec t1 = tr.getChildren().get(0);

        assertNull(tr.getAttrs().get("X"));
        assertNull(t1.getAttrs().get("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodNamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1").onEnter(tracer.begin("TEST1", 0),
                tracer.formatTraceAttr("TEST1", "X", "XXX"))
                .include(spy.byMethod(TCLASS4, "recursive3")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        STRec tr = parseTrace(o.getChunks(), symbols);

        assertEquals("XXX", tr.getAttrs().get("X"));
    }

    @Test
    public void testTraceAttrUpwardPropagationToTheSameMethodUnnamed() throws Exception {
        tracer.include(spy.byMethod(TCLASS4, "recur*"));
        spy.add(spy.instance("1")
                .onEnter(tracer.begin("TEST1", 0),
                        tracer.formatTraceAttr(null, "X", "XXX"))
                .include(spy.byMethod(TCLASS4, "recursive3")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS4);
        invoke(obj, "recursive3");

        STRec tr = parseTrace(o.getChunks(), symbols);

        assertEquals("XXX", tr.getAttrs().get("X"));
    }

    // TODO testGetTraceAttrFromUpperAndCurrentFrame

    // TODO testTraceFlagsMark

    // TODO testTraceFlagsUpwardPropagation

    // TODO testInTraceCheckerPositiveCheck

    // TODO testInTraceCheckerNegativeCheck

    // TODO testTraceSpyMethodsFlagOn

    // TODO testTraceSpyMethodsFlagOff

    // TODO testTraceSpyMethodsFlagOffAndOneMethodManuallyAddedToTracer

    // TODO testTraceExceptionThrowMarkError

    // TODO testTraceExceptionUnmarkError

    @Test
    public void testTraceExceptionThrowCheckError() throws Exception {
        tracer.include(spy.byMethod(TCLASS1, "errorMethod"));

        spy.add(spy.instance("1").onEnter(tracer.begin("TEST", 0))
                .include(spy.byMethod(TCLASS1, "errorMethod")));

        TraceHandler.setMinMethodTime(0);
        tracer.output(o);

        Object obj = instantiate(agentInstance.getClassTransformer(), TCLASS1);

        invoke(obj, "errorMethod");

        STRec tr = parseTrace(o.getChunks(), symbols);
        STErr err = tr.getError();
        assertNotNull(err);
        assertEquals("java.lang.NullPointerException", err.getClassName());

    }

}
