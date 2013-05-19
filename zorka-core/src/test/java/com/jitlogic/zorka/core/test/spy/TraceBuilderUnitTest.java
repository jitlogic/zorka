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

package com.jitlogic.zorka.core.test.spy;

import com.jitlogic.zorka.core.perfmon.Submittable;
import com.jitlogic.zorka.core.store.SimpleSymbolRegistry;
import com.jitlogic.zorka.core.store.SymbolRegistry;
import com.jitlogic.zorka.core.util.SymbolicException;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.*;
import com.jitlogic.zorka.core.test.spy.support.TestTracer;

import com.jitlogic.zorka.core.test.support.TestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

public class TraceBuilderUnitTest extends ZorkaFixture {

    private TestTracer output = new TestTracer();
    private SymbolRegistry symbols = new SimpleSymbolRegistry(null);
    private List<TraceRecord> records = new ArrayList<TraceRecord>();

    private TraceBuilder b = new TraceBuilder(
        new TracerOutput() {
            @Override public void submit(Submittable obj) { obj.traverse(output); records.add((TraceRecord)obj); }
        }, symbols);

    private static final int MS = 1000000;

    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int m2 = symbols.symbolId("otherMethod");
    private int m3 = symbols.symbolId("anotherMethod");
    private int s1 = symbols.symbolId("()V");
    private int t1 = symbols.symbolId("TRACE1");
    private int t2 = symbols.symbolId("TRACE2");
    private int a1 = symbols.symbolId("ATTR1");
    private int a2 = symbols.symbolId("ATTR2");

    @After
    public void tearDown() {
        tracer.setTracerMaxTraceRecords(4096);
        tracer.setTracerMinMethodTime(250000);
        tracer.setTracerMinTraceTime(50);
    }

    @Test
    public void testStrayTraceFragment() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceReturn(200 * MS);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleTraceWithOneShortElement() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceReturn(100 * MS + 100);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleTraceWithOneShortElementAndAlwaysSubmitFlag() throws Exception {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlag(TraceMarker.SUBMIT_TRACE);
        b.traceReturn(20 * MS);

        Assert.assertEquals("Output actions mismatch.",
                Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceReturn"),
                output.listAttr("action"));
    }


    @Test
    public void testAllMethodsSubmitFlag() throws Exception {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlag(TraceMarker.ALL_METHODS);
        b.traceEnter(c1, m1, s1, 20 * MS);
        b.traceReturn(20 * MS + 10);
        b.traceReturn(200 * MS);

        Assert.assertEquals("Output actions mismatch.",
                Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                        "traceReturn", "traceReturn"), output.listAttr("action"));

    }


    @Test
    public void testSingleOneElementTrace() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 200L, TraceMarker.DROP_INTERIM);
        b.traceReturn(200 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceReturn"),
                          output.listAttr("action"));
    }


    @Test
    public void testSingleTraceWithOneChildElement() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 300L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 200 * MS);
        b.traceReturn(300 * MS);
        b.traceReturn(400 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceReturn", "traceReturn"), output.listAttr("action"));

        assertEquals("should record two calls", 2L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testTraceWithErrorElement() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 400L, TraceMarker.DROP_INTERIM);
        b.traceError(new Exception("oja!"), 200 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceError"),
                          output.listAttr("action"));
    }


    @Test
    public void testTraceWithShortErrorChildElement() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 500L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 200 * MS);
        b.traceError(new Exception("oja!"), 200 * MS + 100);
        b.traceReturn(400 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceError", "traceReturn"), output.listAttr("action"));
        assertEquals("should record two calls", 2L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testMixedTraceWithSomeShortElementsAndSomeErrors() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 600L, TraceMarker.DROP_INTERIM);
        b.setMinimumTraceTime(0);
        b.traceEnter(c1, m2, s1, 110 * MS);
        b.traceReturn(110 * MS + 100);
        b.traceEnter(c1, m2, s1, 120 * MS);
        b.traceReturn(130 * MS);
        b.traceReturn(140 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceReturn", "traceReturn"), output.listAttr("action"));

        assertEquals("Number of recorded calls.", 3L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testAttrsEncode() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 700L, TraceMarker.DROP_INTERIM);
        b.setMinimumTraceTime(0);
        b.newAttr(a1, "some val");
        b.newAttr(a2, "other val");
        b.traceReturn(110 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "newAttr", "newAttr", "traceReturn"),
            output.listAttr("action"));
    }


    @Test
    public void testSkipTraceRecordsIfNoMarkerIsSet() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceEnter(c1, m2, s1, 100 * MS);

        TraceRecord top = TestUtil.getField(b, "ttop");
        Assert.assertTrue("Trace record top should have no parent.", top.getParent() == null);
    }


    @Test
    public void testTraceRecordLimitHorizontal() throws Exception {
        tracer.setTracerMaxTraceRecords(3);
        tracer.setTracerMinTraceTime(0);

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);

        b.traceEnter(c1, m2, s1, 3 * MS);
        b.traceReturn(4 * MS);
        b.traceEnter(c1, m2, s1, 5 * MS);
        b.traceReturn(6 * MS);
        b.traceEnter(c1, m2, s1, 7 * MS);
        b.traceReturn(8 * MS);
        b.traceEnter(c1, m2, s1, 9 * MS);
        b.traceReturn(10 * MS);


        TraceRecord top = TestUtil.getField(b, "ttop");
        assertEquals("Should limit to 2 children (plus parent)",
                2, top.numChildren());

        b.traceReturn(11 * MS);

        Assert.assertEquals("Should record begin and 3 full records", 1 +3*3, output.size());
        output.check(2, "calls", 5L);
        output.check(0, "flags", TraceMarker.OVERFLOW_FLAG|TraceMarker.DROP_INTERIM);
    }


    @Test
    public void testTraceRecordLimitVertical() throws Exception {
        tracer.setTracerMaxTraceRecords(3);
        tracer.setTracerMinTraceTime(0);

        // Start new trace
        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);

        // Recursively enter 3 times
        b.traceEnter(c1, m2, s1, 3 * MS);
        b.traceEnter(c1, m2, s1, 4 * MS);
        b.traceEnter(c1, m2, s1, 5 * MS);
        b.traceEnter(c1, m2, s1, 6 * MS);
        b.traceReturn(7 * MS);
        b.traceReturn(8 * MS);
        b.traceReturn(9 * MS);
        b.traceReturn(10 * MS);

        TraceRecord top = TestUtil.getField(b, "ttop");
        assertEquals("Root record of a trace should have one child.", 1, top.numChildren());

        b.traceReturn(11 * MS);

        Assert.assertEquals("Should record begin and 3 full records", 1 +3*3, output.size());

        output.check(0, "flags", TraceMarker.OVERFLOW_FLAG|TraceMarker.DROP_INTERIM);
    }


    @Test
    public void testTraceRecordLimitCrossingMarkers() throws Exception {
        tracer.setTracerMaxTraceRecords(4);
        tracer.setTracerMinTraceTime(0);

        // Start new trace
        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);

        // Start subsequent trace
        b.traceEnter(c1, m2, s1, 3 * MS);
        b.traceBegin(t2, 4 * MS, TraceMarker.DROP_INTERIM);

        // Submit some records, so b will reach limit
        b.traceEnter(c1, m2, s1, 5 * MS);
        b.traceReturn(6 * MS);
        b.traceEnter(c1, m2, s1, 7 * MS);
        b.traceReturn(8 * MS);
        b.traceEnter(c1, m2, s1, 9 * MS);
        b.traceReturn(10 * MS);

        // Return back to trace root frame
        b.traceReturn(11 * MS);

        // Check inner trace
        TraceRecord top = TestUtil.getField(b, "ttop");
        assertEquals("Root record of a trace should have only one child.", 1, top.numChildren());
        Assert.assertEquals("Should record begin and 3 full frames (3 records each)",
                1 + 3 * 3, output.size());

        output.clear();

        b.traceReturn(16 * MS);

        Assert.assertEquals("Should record 2 times begin and 4 full frames (3 records each)",
                2 + 4 * 3, output.size());

        output.check(0, "flags", TraceMarker.OVERFLOW_FLAG|TraceMarker.DROP_INTERIM);
    }


    @Test
    public void testTraceWithMultipleBeginFlags() throws Exception {
        tracer.setTracerMinTraceTime(0);
        ZorkaLogger.setTracerLevel(0); // TODO check why ZorkaLog objects are not constructed correctly in this test

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceBegin(t2, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceReturn(3 * MS);

        Assert.assertEquals("Should record one begin event and one full frame (3 records)", 1 + 3, output.size());
        output.check(0, "action", "traceBegin", "traceId", t1);
    }


    @Test
    public void testTraceWithTooManyReturns() throws Exception {
        tracer.setTracerMinTraceTime(0);

        // Submit one frame
        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceReturn(3 * MS);

        // Malicious traceReturn event
        b.traceReturn(4 * MS);

        // Submit another frame
        b.traceEnter(c1, m1, s1, 5 * MS);
        b.traceBegin(t1, 6 * MS, TraceMarker.DROP_INTERIM);
        b.traceReturn(7 * MS);

        Assert.assertEquals("Should record one begin event and one full frame (3 records)", 2 * (1 + 3), output.size());
    }


    @Test
    public void testSingleTraceWithMultipleEmbeddedTracesInside() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(0);

        // Submit one frame
        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);

        // Start subsequent trace
        b.traceEnter(c1, m2, s1, 3 * MS);
        b.traceBegin(t2, 4 * MS, TraceMarker.DROP_INTERIM);
        b.traceReturn(5 * MS);

        // Single trace should appear on output
        Assert.assertEquals(4, output.size());
        output.clear();

        // Start subsequent trace
        b.traceEnter(c1, m2, s1, 6 * MS);
        b.traceBegin(t2, 7 * MS, 0);
        b.traceReturn(8 * MS);

        Assert.assertEquals(4, output.size());
        output.clear();

        // Return from main frame
        b.traceReturn(9 * MS);

        Assert.assertEquals(3+3*3, output.size());
    }


    @Test
    public void testExceptionObjectCleanupAndMarkIfPassedThrough() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(0);

        Exception e = new Exception("oja!");

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 3 * MS);

        b.traceError(e, 4 * MS);
        b.traceError(e, 5 * MS);

        Assert.assertEquals(7, output.size());

        // Exception of inner method
        output.check(5, "exception", new SymbolicException(e, symbols, true));

        // Exception of outer method
        output.check(6, "exception", null);

        // Flags of outer method
        output.check(2, "flags", TraceRecord.EXCEPTION_PASS|TraceRecord.TRACE_BEGIN);

    }


    @Test
    public void testThrowAndWrapExceptionCheckIfWrappingExceptionIsMarked() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(0);

        Exception e1 = new Exception("oja!");
        Exception e2 = new Exception("OJA!", e1);

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 3 * MS);

        b.traceError(e1, 4 * MS);
        b.traceError(e2, 5 * MS);

        Assert.assertEquals(7, output.size());

        // Exception of inner method
        output.check(5, "exception", new SymbolicException(e1, symbols, true));

        // Exception of outer method
        output.check(6, "exception", new SymbolicException(e2, symbols, false));

        // Flags of outer method
        output.check(2, "flags", TraceRecord.EXCEPTION_WRAP|TraceRecord.TRACE_BEGIN);
    }


    @Test
    public void testTimeCalculationAfterShortMethodDrop() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);

        b.traceEnter(c1, m2, s1, 3);
        b.traceReturn(4);

        b.traceEnter(c1, m2, s1, 10);
        b.traceReturn(25);

        b.traceReturn(56);

        output.check(2, "action", "traceStats", "calls", 3L);
        output.check(3, "action", "traceEnter", "tstamp", 0L);
        output.check(5, "action", "traceReturn", "tstamp", 15L);
    }


    @Test
    public void testTimeCalculationAfterShortInnerMethodDrop() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);

        b.traceEnter(c1, m2, s1, 2);
        b.traceReturn(3);

        b.traceEnter(c1, m2, s1, 4);
        b.traceEnter(c1, m2, s1, 5);
        b.traceReturn(6);
        b.traceReturn(20);

        b.traceReturn(40);

        output.check(3, "action", "traceEnter", "tstamp", 0L);
        output.check(5, "action", "traceReturn", "tstamp", 16L);
    }


    @Test
    public void testMaintainRecordNumCounter() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);
        assertEquals(1, TestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 2);
        assertEquals(2, TestUtil.getField(b, "numRecords"));

        b.traceReturn(3);
        assertEquals(1, TestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 4);
        assertEquals(2, TestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 5);
        assertEquals(3, TestUtil.getField(b, "numRecords"));

        b.traceReturn(6);
        assertEquals(2, TestUtil.getField(b, "numRecords"));

        b.traceReturn(20);
        assertEquals(2, TestUtil.getField(b, "numRecords"));

        b.traceReturn(40);
        assertEquals(0, TestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m1, s1, 41);
        assertEquals(1, TestUtil.getField(b, "numRecords"));
    }


    @Test
    public void testFilterOutShortInterimMethods() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 2);
        b.traceEnter(c1, m3, s1, 3);

        b.traceReturn(20);
        b.traceReturn(21);
        b.traceReturn(22);

        Assert.assertEquals(7, output.size());

        Assert.assertEquals("Output actions mismatch.",
                Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                        "traceReturn", "traceReturn"), output.listAttr("action"));

        assertEquals("should record 3 calls", 3L, output.getData().get(2).get("calls"));
        assertEquals("should mark dropped record", TraceRecord.DROPPED_PARENT, output.getData().get(4).get("flags"));
    }

    @Test
    public void testProperExceptionCleanupAfterTraceExit() throws Exception {

        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceError(new Exception("oja!"), 300);
        b.traceEnter(c1, m1, s1, 100);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceReturn(200);

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getException()).isNull();
    }

    @Test
    public void testTraceDropFlag() throws Exception {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlag(TraceMarker.DROP_TRACE);
        b.traceReturn(20 * MS);

        assertThat(records.size()).isEqualTo(0);
    }

}
