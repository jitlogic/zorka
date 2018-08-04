/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.core.spy.*;

import com.jitlogic.zorka.core.test.support.CoreTestUtil;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TraceBuilderUnitTest extends ZorkaFixture {

    private SymbolRegistry symbols = new SymbolRegistry();
    private List<TraceRecord> records = new ArrayList<TraceRecord>();

    private TraceBuilder b = new TraceBuilder(
            new ZorkaSubmitter<SymbolicRecord>() {
                @Override
                public boolean submit(SymbolicRecord obj) {
                    return records.add((TraceRecord) obj);
                }
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

    @Before
    public void setUp() {
        // TODO get rid of static settings
        tracer.setTracerMaxTraceRecords(4096);
        tracer.setTracerMinMethodTime(250000);
        tracer.setTracerMinTraceTime(50);
    }


    @After
    public void tearDown() {
        tracer.setTracerMaxTraceRecords(4096);
        tracer.setTracerMinMethodTime(250000);
        tracer.setTracerMinTraceTime(50);
    }

    private void checkRC(int recs, int...chld) {
        assertEquals(records.size(), recs);
        if (chld.length > 0) {
            TraceRecord rec = records.get(0);
            for (int c : chld) {
                assertEquals(rec.numChildren(), c);
                rec = rec.getChild(0);
            }
        }
    }

    @Test
    public void testStrayTraceFragment() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceReturn(200 * MS);

        checkRC(0);
    }


    @Test
    public void testSingleTraceWithOneShortElement() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceReturn(100 * MS + 100);

        checkRC(0);
    }


    @Test
    public void testSingleTraceWithOneShortElementAndAlwaysSubmitFlag() {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlags(0, TraceMarker.SUBMIT_TRACE);
        b.traceReturn(20 * MS);

        checkRC(1, 0);
    }


    @Test
    public void testAllMethodsSubmitFlag() {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlags(0, TraceMarker.ALL_METHODS);
        b.traceEnter(c1, m1, s1, 20 * MS);
        b.traceReturn(20 * MS + 10);
        b.traceReturn(200 * MS);

        checkRC(1, 1, 0);
    }

    @Test
    public void testForceTraceRecordFlag() {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1,5L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 20 * MS);
        b.markRecordFlags(TraceRecord.FORCE_TRACE);
        b.traceReturn(20 * MS + 10);
        b.traceReturn(200 * MS);

        checkRC(1, 1, 0);
    }

    @Test
    public void testSingleOneElementTrace() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 200L, TraceMarker.DROP_INTERIM);
        b.traceReturn(200 * MS);

        checkRC(1, 0);
    }


    @Test
    public void testSingleTraceWithOneChildElement() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 300L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 200 * MS);
        b.traceReturn(300 * MS);
        b.traceReturn(400 * MS);

        checkRC(1, 1, 0);
        assertEquals(2, records.get(0).getCalls());
    }


    @Test
    public void testTraceWithErrorElement() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 400L, TraceMarker.DROP_INTERIM);
        b.traceError(new Exception("oja!"), 200 * MS);

        checkRC(1, 0);
        assertNotNull(records.get(0).getException());
    }


    @Test
    public void testTraceWithShortErrorChildElement() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 500L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 200 * MS);
        b.traceError(new Exception("oja!"), 200 * MS + 100);
        b.traceReturn(400 * MS);

        checkRC(1, 1, 0);
        assertEquals(2L, records.get(0).getCalls());
    }


    @Test
    public void testMixedTraceWithSomeShortElementsAndSomeErrors() {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 600L, TraceMarker.DROP_INTERIM);
        b.setMinimumTraceTime(0);
        b.traceEnter(c1, m2, s1, 110 * MS);
        b.traceReturn(110 * MS + 100);
        b.traceEnter(c1, m2, s1, 120 * MS);
        b.traceReturn(130 * MS);
        b.traceReturn(140 * MS);

        checkRC(1, 1, 0);
        assertEquals(3L, records.get(0).getCalls());
    }


    @Test
    public void testAttrsEncode() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceBegin(t1, 700L, TraceMarker.DROP_INTERIM);
        b.setMinimumTraceTime(0);
        b.newAttr(-1, a1, "some val");
        b.newAttr(-1, a2, "other val");
        b.traceReturn(110 * MS);

        checkRC(1, 0);
        assertEquals(records.get(0).numAttrs(), 2);
    }


    @Test
    public void testSkipTraceRecordsIfNoMarkerIsSet() throws Exception {
        b.traceEnter(c1, m1, s1, 100 * MS);
        b.traceEnter(c1, m2, s1, 100 * MS);

        TraceRecord top = CoreTestUtil.getField(b, "ttop");
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


        TraceRecord top = CoreTestUtil.getField(b, "ttop");
        assertEquals("Should limit to 2 children (plus parent)",
                2, top.numChildren());

        b.traceReturn(11 * MS);

        checkRC(1, 2);
        assertEquals(5L, records.get(0).getCalls());
        assertEquals(TraceMarker.OVERFLOW_FLAG | TraceMarker.DROP_INTERIM, records.get(0).getMarker().getFlags());
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

        TraceRecord top = CoreTestUtil.getField(b, "ttop");
        assertEquals("Root record of a trace should have one child.", 1, top.numChildren());

        b.traceReturn(11 * MS);

        checkRC(1, 1, 1, 0);
        assertEquals(TraceMarker.OVERFLOW_FLAG | TraceMarker.DROP_INTERIM, records.get(0).getMarker().getFlags());
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
        TraceRecord top = CoreTestUtil.getField(b, "ttop");
        assertEquals("Root record of a trace should have only one child.", 1, top.numChildren());
        checkRC(1, 2, 0);

        records.clear();

        b.traceReturn(16 * MS);

        checkRC(1, 1, 2, 0);

        assertEquals(TraceMarker.OVERFLOW_FLAG | TraceMarker.DROP_INTERIM, records.get(0).getMarker().getFlags());
    }


    @Test
    public void testTraceWithMultipleBeginFlags() {
        tracer.setTracerMinTraceTime(0);

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceBegin(t2, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceReturn(3 * MS);

        checkRC(1, 0);
        assertEquals(t1, records.get(0).getMarker().getTraceId());
    }


    @Test
    public void testTraceWithTooManyReturns() {
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

        checkRC(2);
    }


    @Test
    public void testSingleTraceWithMultipleEmbeddedTracesInside() {
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
        checkRC(1, 0);
        records.clear();

        // Start subsequent trace
        b.traceEnter(c1, m2, s1, 6 * MS);
        b.traceBegin(t2, 7 * MS, 0);
        b.traceReturn(8 * MS);

        checkRC(1, 0);
        records.clear();

        // Return from main frame
        b.traceReturn(9 * MS);

        checkRC(1, 2);
    }


    @Test
    public void testExceptionObjectCleanupAndMarkIfPassedThrough() {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(0);

        Exception e = new Exception("oja!");

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 3 * MS);

        b.traceError(e, 4 * MS);
        b.traceError(e, 5 * MS);

        checkRC(1, 1, 0);

        assertNull(records.get(0).getException());
        assertEquals(TraceRecord.EXCEPTION_PASS | TraceRecord.TRACE_BEGIN, records.get(0).getFlags());
        assertEquals(new SymbolicException(e, symbols, true), records.get(0).getChild(0).getException());
    }


    @Test
    public void testThrowAndWrapExceptionCheckIfWrappingExceptionIsMarked() {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(0);

        Exception e1 = new Exception("oja!");
        Exception e2 = new Exception("OJA!", e1);

        b.traceEnter(c1, m1, s1, 1 * MS);
        b.traceBegin(t1, 2 * MS, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 3 * MS);

        b.traceError(e1, 4 * MS);
        b.traceError(e2, 5 * MS);

        checkRC(1, 1, 0);

        assertEquals(new SymbolicException(e1, symbols, true), records.get(0).getChild(0).getException());
        assertEquals(new SymbolicException(e2, symbols, false), records.get(0).getException());
        assertEquals(TraceRecord.EXCEPTION_WRAP | TraceRecord.TRACE_BEGIN, records.get(0).getFlags());
    }


    @Test
    public void testTimeCalculationAfterShortMethodDrop() {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);

        b.traceEnter(c1, m2, s1, 3);
        b.traceReturn(4);

        b.traceEnter(c1, m2, s1, 10);
        b.traceReturn(25);

        b.traceReturn(56);

        TraceRecord tr = records.get(0);

        assertEquals(3, tr.getCalls());
        assertEquals(15L, tr.getChild(0).getTime());
    }


    @Test
    public void testTimeCalculationAfterShortInnerMethodDrop() {
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

        checkRC(1, 1, 0);
        assertEquals(16L, records.get(0).getChild(0).getTime());
    }


    @Test
    public void testMaintainRecordNumCounter() throws Exception {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);
        assertEquals((Integer)1, CoreTestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 2);
        assertEquals((Integer)2, CoreTestUtil.getField(b, "numRecords"));

        b.traceReturn(3);
        assertEquals((Integer)1, CoreTestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 4);
        assertEquals((Integer)2, CoreTestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m2, s1, 5);
        assertEquals((Integer)3, CoreTestUtil.getField(b, "numRecords"));

        b.traceReturn(6);
        assertEquals((Integer)2, CoreTestUtil.getField(b, "numRecords"));

        b.traceReturn(20);
        assertEquals((Integer)2, CoreTestUtil.getField(b, "numRecords"));

        b.traceReturn(40);
        assertEquals((Integer)0, CoreTestUtil.getField(b, "numRecords"));

        b.traceEnter(c1, m1, s1, 41);
        assertEquals((Integer)1, CoreTestUtil.getField(b, "numRecords"));
    }


    @Test
    public void testFilterOutShortInterimMethods() {
        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceEnter(c1, m1, s1, 1);
        b.traceBegin(t1, 2, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m2, s1, 2);
        b.traceEnter(c1, m3, s1, 3);

        b.traceReturn(20);
        b.traceReturn(21);
        b.traceReturn(22);

        checkRC(1, 1, 0);
        assertEquals(3L, records.get(0).getCalls());
        assertEquals(TraceRecord.DROPPED_PARENT, records.get(0).getChild(0).getFlags());
    }


    @Test
    public void testProperExceptionCleanupAfterTraceExit() {

        tracer.setTracerMinTraceTime(0);
        tracer.setTracerMinMethodTime(10);

        b.traceError(new Exception("oja!"), 300);
        b.traceEnter(c1, m1, s1, 100);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceReturn(200);

        assertEquals(1, records.size());
        assertNull(records.get(0).getException());
    }


    @Test
    public void testTraceDropFlag() {
        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.markTraceFlags(0, TraceMarker.DROP_TRACE);
        b.traceReturn(20 * MS);

        assertEquals(0, records.size());
    }


    @Test
    public void testTraceAttrReusableRecordBug() {
        tracer.setTracerMinTraceTime(0);

        b.traceEnter(c1, m1, s1, 10 * MS);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m1, s1, 10 * MS + 1);
        b.traceReturn(10 * MS + 2);
        b.newAttr(-1, 1, "oja!");
        b.traceReturn(20 * MS);

        checkRC(1, 0);
        assertEquals(1, records.get(0).numAttrs());
    }

    @Test
    public void testTraceSubmitWhenMinTraceCallsExceeded() {
        tracer.setTracerMinMethodTime(10);
        tracer.setTracerMinTraceCalls(3);

        b.traceEnter(c1, m1, s1, 100L);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m1, s1, 110L);
        b.traceEnter(c1, m1, s1, 120L);
        b.traceReturn(130L);
        b.traceReturn(140L);
        b.traceReturn(150L);

        assertEquals(1, records.size());
    }

    @Test
    public void testTraceSubmitWhenMinTraceCallsNotExceeded() {
        tracer.setTracerMinMethodTime(10);
        tracer.setTracerMinTraceCalls(3);

        b.traceEnter(c1, m1, s1, 100L);
        b.traceBegin(t1, 100L, TraceMarker.DROP_INTERIM);
        b.traceEnter(c1, m1, s1, 110L);
        b.traceReturn(140L);
        b.traceReturn(150L);

        assertEquals(0, records.size());
    }

}
