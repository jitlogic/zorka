package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DTraceComponentsUnitTest extends ZorkaFixture {

    @Test
    public void testTraceInputProcessingInitialTrace() {
        SpyProcessor dti = tracer.dtraceInput();

        Map<String,Object> rec = new HashMap<String, Object>();

        assertSame(rec, dti.process(rec));

        DTraceState ds = (DTraceState)rec.get("DTRACE");
        assertNotNull(ds);

        assertNotNull(ds.getUuid());
        assertNotNull(ds.getTid());

        TraceRecord tr = agentInstance.getTracer().getHandler().realTop();
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_IN")));
    }

    @Test
    public void testTraceInputProcessingContinuation() {
        SpyProcessor dti = tracer.dtraceInput();

        Map<String,Object> rec = new HashMap<String, Object>();

        String uuid = UUID.randomUUID().toString();

        rec.put("DTRACE_UUID", uuid);
        rec.put("DTRACE_IN", "_1");

        assertSame(rec, dti.process(rec));

        DTraceState ds = (DTraceState)rec.get("DTRACE");
        assertNotNull(ds);

        assertEquals(uuid, ds.getUuid());
        assertEquals("_1", ds.getTid());

        TraceRecord tr = agentInstance.getTracer().getHandler().realTop();
        assertNotNull(tr);
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid() + "_1", tr.getAttr(symbols.symbolId("DTRACE_IN")));
    }

    @Test
    public void testTraceOutputProcessing() {
        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
        DTraceState ds = new DTraceState(tracer, UUID.randomUUID().toString(), "_1", null);
        tlds.set(ds);

        Map<String,Object> rec = new HashMap<String, Object>();
        SpyProcessor dto = tracer.dtraceOutput();

        assertSame(rec, dto.process(rec));

        DTraceState ds1 = (DTraceState)rec.get("DTRACE");
        assertSame(ds1, ds);

        assertEquals("_1_1", rec.get("DTRACE_OUT"));

        TraceRecord tr = agentInstance.getTracer().getHandler().realTop();
        assertNotNull(tr);
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid() + "_1_1", tr.getAttr(symbols.symbolId("DTRACE_OUT")));

    }
}
