package com.jitlogic.zorka.core.test.tracer;

import com.jitlogic.zorka.common.ZorkaSubmitter;
import com.jitlogic.zorka.common.collector.TraceChunkData;
import com.jitlogic.zorka.common.collector.TraceDataExtractor;
import com.jitlogic.zorka.common.collector.TraceDataResult;
import com.jitlogic.zorka.common.tracedata.SymbolicRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.output.LTraceHttpOutput;
import com.jitlogic.zorka.common.collector.MemoryCollector;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.jitlogic.zorka.core.test.support.BytecodeInstrumentationFixture.TCLASS1;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.instantiate;
import static com.jitlogic.zorka.core.test.support.CoreTestUtil.invoke;
import static org.junit.Assert.*;

public class LTracerCollectUnitTest extends ZorkaFixture {

    private MemoryCollector collector;
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
        collector = new MemoryCollector();
        output = new LTraceHttpOutput(config, HTTP_CONF, symbols, collector);
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
        assertEquals(1, collector.getStore().size());

        TraceChunkData cd = collector.getStore().get(0);
        assertNotNull(cd.getTraceData());
        assertTrue(cd.getTraceData().length > 0);

        TraceDataExtractor extractor = new TraceDataExtractor(collector.getRegistry());
        TraceDataResult tr = extractor.extract(Collections.singletonList(cd));
        assertNotNull(tr);
        assertNotEquals("<?>", tr.getMethod());
        assertEquals(TCLASS1 + ".trivialMethod()", tr.getMethod());
        assertEquals("TEST", tr.getTraceType());
        assertEquals("TEST", tr.getAttr("component"));
        assertNotNull(tr.getAttr("thread.name"));
        assertNotNull(tr.getAttr("thread.id"));
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
        assertEquals(1, collector.getStore().size());

        TraceChunkData cd = collector.getStore().get(0);
        assertNotNull(cd.getTraceData());
        assertTrue(cd.getTraceData().length > 0);

        TraceDataExtractor extractor = new TraceDataExtractor(collector.getRegistry());
        TraceDataResult tr = extractor.extract(Collections.singletonList(cd));
        assertNotNull(tr);

        assertEquals(TCLASS1 + ".trivialStrMethod()", tr.getMethod());
        assertEquals("BUMBUM", tr.getTraceType());
        assertEquals(1, tr.getCalls());
    }

}
