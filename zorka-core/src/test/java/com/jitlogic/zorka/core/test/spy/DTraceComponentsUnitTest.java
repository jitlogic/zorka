package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.core.spy.DTraceState;
import com.jitlogic.zorka.core.spy.ltracer.LTracer;
import com.jitlogic.zorka.core.spy.ltracer.LTracerLib;
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
        SpyProcessor dti = tracer.dtraceInput(42);

        Map<String,Object> rec = new HashMap<String, Object>();
        rec.put("T1", 100L);

        assertSame(rec, dti.process(rec));

        DTraceState ds = (DTraceState)rec.get("DTRACE");
        assertNotNull(ds);

        assertNotNull(ds.getUuid());
        assertNotNull(ds.getTid());
        assertEquals(42L, ds.getThreshold());
        assertEquals(100L, ds.getTstart());

        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_IN")));
    }

    @Test
    public void testTraceInputProcessingContinuation() {
        SpyProcessor dti = tracer.dtraceInput(-1);

        Map<String,Object> rec = new HashMap<String, Object>();

        String uuid = UUID.randomUUID().toString();

        rec.put("DTRACE_UUID", uuid);
        rec.put("DTRACE_IN", "_1");

        assertSame(rec, dti.process(rec));

        DTraceState ds = (DTraceState)rec.get("DTRACE");
        assertNotNull(ds);

        assertEquals(uuid, ds.getUuid());
        assertEquals("_1", ds.getTid());
        assertEquals(-1L, ds.getThreshold());

        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
        assertNotNull(tr);
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid() + "_1", tr.getAttr(symbols.symbolId("DTRACE_IN")));
    }

    @Test
    public void testTraceInputWithThresholdData() {
        SpyProcessor dti = tracer.dtraceInput(-1);

        Map<String,Object> rec = new HashMap<String, Object>();
        rec.put("DTRACE_XTT", "42");

        assertSame(rec, dti.process(rec));

        DTraceState ds = (DTraceState)rec.get("DTRACE");
        assertNotNull(ds);

        assertEquals(42L, ds.getThreshold());
    }

    @Test
    public void testTraceOutputProcessing() {
        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
        DTraceState ds = new DTraceState(tracer, UUID.randomUUID().toString(), "_1",  42L,-1);
        tlds.set(ds);

        Map<String,Object> rec = new HashMap<String, Object>();
        SpyProcessor dto = tracer.dtraceOutput();

        assertSame(rec, dto.process(rec));

        DTraceState ds1 = (DTraceState)rec.get("DTRACE");
        assertSame(ds1, ds);

        assertEquals("_1_1", rec.get("DTRACE_OUT"));

        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
        assertNotNull(tr);
        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
        assertEquals(ds.getUuid() + "_1_1", tr.getAttr(symbols.symbolId("DTRACE_OUT")));
    }

    @Test
    public void testTraceOutputWithThresholdData() {
        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
        DTraceState ds = new DTraceState(tracer, UUID.randomUUID().toString(), "_1",  42000000L,100);
        tlds.set(ds);

        Map<String,Object> rec = new HashMap<String, Object>();
        rec.put("T1", 92000000L);

        SpyProcessor dto = tracer.dtraceOutput();

        assertSame(rec, dto.process(rec));
        assertEquals(50L, rec.get("DTRACE_XTT"));
    }

    @Test
    public void testCleanupForceSubmit() {
        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
        DTraceState ds = new DTraceState(tracer, UUID.randomUUID().toString(), "_1",  42L,10);
        tlds.set(ds);

        Map<String,Object> rec = new HashMap<String, Object>();
        rec.put("T1", 100L);
        rec.put("T2", 100000000L);

        SpyProcessor dtc = tracer.dtraceClean();

        agentInstance.getTracer().getHandler().traceBegin(symbols.symbolId("HTTP"), 100, 0);
        assertSame(rec, dtc.process(rec));

        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
        assertNotNull(tr);
        assertNotNull(tr.getMarker());

        assertTrue(0 != (tr.getMarker().getFlags() & LTracerLib.SUBMIT_TRACE));
    }
}
