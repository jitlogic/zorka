package com.jitlogic.zorka.core.test.tracer;

import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.tracedata.DTraceContext;
import com.jitlogic.zorka.core.spy.ltracer.LTraceHandler;
import com.jitlogic.zorka.core.spy.ltracer.LTracer;
import com.jitlogic.zorka.core.spy.SpyProcessor;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;

import org.junit.Test;

import static com.jitlogic.zorka.core.spy.TracerLib.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class DTraceComponentsUnitTest extends ZorkaFixture {

    @Test
    public void testTraceInputOutputProcessingInitialTraceJaeger() {
        SpyProcessor dti = tracer.dtraceInput(DJM_JAEGER, DFK_SERVER);

        Map<String,Object> r1 = new HashMap<String, Object>();
        r1.put("T1", 100L);

        LTraceHandler th = ((LTracer) (agentInstance.getTracer())).getLtHandler();
        th.traceEnter(42, 42L);
        th.traceBegin(sid("HTTP"), 24L, 0);

        assertSame(r1, dti.process(r1));

        DTraceContext ds1 = th.getDTraceState();
        assertNotNull(ds1);

        assertNotEquals(0, ds1.getTraceId1());
        assertNotEquals(0, ds1.getTraceId2());
        assertNotNull(ds1.getTraceIdHex());
        assertNotEquals(0, ds1.getSpanId());
        assertNotNull(ds1.getSpanIdHex());
        assertEquals(0, ds1.getParentId());
        assertNotEquals(0, ds1.getTstart());



        SpyProcessor dto = tracer.dtraceOutput(0, 0);
        Map<String,Object> r2 = new HashMap<String, Object>();

        th.traceEnter(42, 42L);
        th.traceBegin(sid("HTTP_CLI"), 42L, 0);
        dto.process(r2);

        DTraceContext ds2 = th.getDTraceState();
        assertNotNull(ds2);

        String dh2 = (String)r2.get(DH_UBER_TID);

    }

    @Test
    public void testTraceInputProcessingContinuationJaeger() {
        SpyProcessor dti = tracer.dtraceInput(-1, DFK_SERVER);

        Map<String,Object> rec = new HashMap<String, Object>();
        rec.put(DH_UBER_TID, "6e05fa04e3167fd5406bbcb9245dc73e:bb153ab2e12b8b15:aa153ab2e12b8b15:00");

        LTraceHandler th = ((LTracer) (agentInstance.getTracer())).getLtHandler();
        th.traceEnter(42, 42L);
        th.traceBegin(sid("HTTP"), 24L, 0);

        assertSame(rec, dti.process(rec));


        DTraceContext ds = th.getDTraceState();

        assertNotNull(ds);
        assertEquals("6e05fa04e3167fd5406bbcb9245dc73e", ds.getTraceIdHex());

        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
        assertNotNull(tr);
    }

    // TODO continuation dla zipkin

    // TODO continuation dla zipkin/b3

    // TODO continuation dla w3c

    // TODO test na flagę SAMPLED=1

    // TODO test na flagę SAMPLED=0

    // TODO test na flagę DEBUG=1

//
//    @Test
//    public void testTraceOutputProcessing() {
//        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
//        DTraceState ds = new DTraceState(UUID.randomUUID().toString(), "_1",  42L,-1);
//        tlds.set(ds);
//
//        Map<String,Object> rec = new HashMap<String, Object>();
//        SpyProcessor dto = tracer.dtraceOutput();
//
//        assertSame(rec, dto.process(rec));
//
//        DTraceState ds1 = (DTraceState)rec.get("DTRACE");
//        assertSame(ds1, ds);
//
//        assertEquals("_1_1", rec.get("DTRACE_OUT"));
//
//        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
//        assertNotNull(tr);
//        assertEquals(ds.getUuid(), tr.getAttr(symbols.symbolId("DTRACE_UUID")));
//        assertEquals(ds.getUuid() + "_1_1", tr.getAttr(symbols.symbolId("DTRACE_OUT")));
//    }
//
//    @Test
//    public void testTraceOutputWithThresholdData() {
//        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
//        DTraceState ds = new DTraceState(UUID.randomUUID().toString(), "_1",  42000000L,100);
//        tlds.set(ds);
//
//        Map<String,Object> rec = new HashMap<String, Object>();
//        rec.put("T1", 92000000L);
//
//        SpyProcessor dto = tracer.dtraceOutput();
//
//        assertSame(rec, dto.process(rec));
//        assertEquals(50L, rec.get("DTRACE_XTT"));
//    }
//
//    @Test
//    public void testCleanupForceSubmit() {
//        ThreadLocal<DTraceState> tlds = (ThreadLocal<DTraceState>)util.getField(tracer, "dtraceLocal");
//        DTraceState ds = new DTraceState(UUID.randomUUID().toString(), "_1",  42L,10);
//        tlds.set(ds);
//
//        Map<String,Object> rec = new HashMap<String, Object>();
//        rec.put("T1", 100L);
//        rec.put("T2", 100000000L);
//
//        SpyProcessor dtc = tracer.dtraceClean();
//
//        agentInstance.getTracer().getHandler().traceBegin(symbols.symbolId("HTTP"), 100, 0);
//        assertSame(rec, dtc.process(rec));
//
//        TraceRecord tr = ((LTracer)(agentInstance.getTracer())).getLtHandler().realTop();
//        assertNotNull(tr);
//        assertNotNull(tr.getMarker());
//
//        assertTrue(0 != (tr.getMarker().getFlags() & LTracerLib.SUBMIT_TRACE));
//    }

    // TODO dokładniejsze testowanie

}
