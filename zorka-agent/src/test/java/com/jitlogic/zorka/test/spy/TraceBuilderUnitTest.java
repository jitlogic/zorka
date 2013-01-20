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

package com.jitlogic.zorka.test.spy;

import com.jitlogic.zorka.common.SymbolRegistry;
import com.jitlogic.zorka.spy.*;
import com.jitlogic.zorka.test.spy.support.TestTracer;

import com.jitlogic.zorka.test.support.TestUtil;
import com.jitlogic.zorka.test.support.ZorkaFixture;
import com.jitlogic.zorka.common.ZorkaAsyncThread;
import com.jitlogic.zorka.common.ZorkaLogConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class TraceBuilderUnitTest extends ZorkaFixture {

    private TestTracer output = new TestTracer();
    private SymbolRegistry symbols = new SymbolRegistry();

    private TraceBuilder builder = new TraceBuilder(
        new ZorkaAsyncThread<TraceRecord>("test") {
            @Override public void submit(TraceRecord obj) { obj.traverse(output); }
            @Override protected void process(TraceRecord obj) {  }
        });

    private static final int MS = 1000000;

    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int m2 = symbols.symbolId("otherMethod");
    private int s1 = symbols.symbolId("()V");
    private int t1 = symbols.symbolId("TRACE1");
    private int t2 = symbols.symbolId("TRACE2");
    private int a1 = symbols.symbolId("ATTR1");
    private int a2 = symbols.symbolId("ATTR2");

    @After
    public void tearDown() {
        spy.setTracerMaxTraceRecords(4096);
        spy.setTracerMinMethodTime(250000);
        spy.setTracerMinTraceTime(50);
    }

    @Test
    public void testStrayTraceFragment() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceReturn(200*MS);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleTraceWithOneShortElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 100L);
        builder.traceReturn(100*MS+100);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleOneElementTrace() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 200L);
        builder.traceReturn(200*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceReturn"),
                          output.listAttr("action"));
    }


    @Test
    public void testSingleTraceWithOneChildElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 300L);
        builder.traceEnter(c1, m2, s1, 200*MS);
        builder.traceReturn(300*MS);
        builder.traceReturn(400*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceReturn", "traceReturn"), output.listAttr("action"));

        assertEquals("should record two calls", 2L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testTraceWithErrorElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 400L);
        builder.traceError(new WrappedException(new Exception("oja!")), 200*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceError"),
                          output.listAttr("action"));
    }


    @Test
    public void testTraceWithShortErrorChildElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 500L);
        builder.traceEnter(c1, m2, s1, 200 * MS);
        builder.traceError(new WrappedException(new Exception("oja!")), 200 * MS + 100);
        builder.traceReturn(400 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceError", "traceReturn"), output.listAttr("action"));
        assertEquals("should record two calls", 2L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testMixedTraceWithSomeShortElementsAndSomeErrors() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 600L);
        builder.setMinimumTraceTime(0);
        builder.traceEnter(c1, m2, s1, 110*MS);
        builder.traceReturn(110 * MS + 100);
        builder.traceEnter(c1, m2, s1, 120*MS);
        builder.traceReturn(130*MS);
        builder.traceReturn(140 * MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceEnter", "traceStats",
                          "traceReturn", "traceReturn"), output.listAttr("action"));

        assertEquals("Number of recorded calls.", 3L, output.getData().get(2).get("calls"));
    }


    @Test
    public void testAttrsEncode() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1, 700L);
        builder.setMinimumTraceTime(0);
        builder.newAttr(a1, "some val");
        builder.newAttr(a2, "other val");
        builder.traceReturn(110*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "newAttr", "newAttr", "traceReturn"),
            output.listAttr("action"));
    }


    @Test
    public void testSkipTraceRecordsIfNoMarkerIsSet() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceEnter(c1, m2, s1, 100*MS);

        TraceRecord top = TestUtil.getField(builder, "ttop");
        Assert.assertTrue("Trace record top should have no parent.", top.getParent() == null);
    }


    @Test
    public void testTraceRecordLimitHorizontal() throws Exception {
        spy.setTracerMaxTraceRecords(3);
        spy.setTracerMinTraceTime(0);

        builder.traceEnter(c1, m1, s1, 1 * MS);
        builder.traceBegin(t1, 2 * MS);

        builder.traceEnter(c1, m2, s1, 3*MS);
        builder.traceReturn(4*MS);
        builder.traceEnter(c1, m2, s1, 5*MS);
        builder.traceReturn(6*MS);
        builder.traceEnter(c1, m2, s1, 7*MS);
        builder.traceReturn(8*MS);
        builder.traceEnter(c1, m2, s1, 9*MS);
        builder.traceReturn(10*MS);


        TraceRecord top = TestUtil.getField(builder, "ttop");
        assertEquals("Should limit to 2 children (plus parent)",
                2, top.numChildren());

        builder.traceReturn(11 * MS);

        Assert.assertEquals("Should record traceBegin and 3 full records", 1 +3*3, output.size());
        output.check(2, "calls", 5L, "flags", TraceMarker.OVERFLOW_FLAG);
    }


    @Test
    public void testTraceRecordLimitVertical() throws Exception {
        spy.setTracerMaxTraceRecords(3);
        spy.setTracerMinTraceTime(0);

        // Start new trace
        builder.traceEnter(c1, m1, s1, 1 * MS);
        builder.traceBegin(t1, 2 * MS);

        // Recursively enter 3 times
        builder.traceEnter(c1, m2, s1, 3 * MS);
        builder.traceEnter(c1, m2, s1, 4 * MS);
        builder.traceEnter(c1, m2, s1, 5 * MS);
        builder.traceEnter(c1, m2, s1, 6 * MS);
        builder.traceReturn(7*MS);
        builder.traceReturn(8 * MS);
        builder.traceReturn(9*MS);
        builder.traceReturn(10*MS);

        TraceRecord top = TestUtil.getField(builder, "ttop");
        assertEquals("Root record of a trace should have one child.", 1, top.numChildren());

        builder.traceReturn(11*MS);

        Assert.assertEquals("Should record traceBegin and 3 full records", 1 +3*3, output.size());

        output.check(2, "flags", TraceMarker.OVERFLOW_FLAG);
    }


    @Test
    public void testTraceRecordLimitCrossingMarkers() throws Exception {
        spy.setTracerMaxTraceRecords(4);
        spy.setTracerMinTraceTime(0);

        // Start new trace
        builder.traceEnter(c1, m1, s1, 1*MS);
        builder.traceBegin(t1, 2*MS);

        // Start subsequent trace
        builder.traceEnter(c1, m2, s1, 3*MS);
        builder.traceBegin(t2, 4*MS);

        // Submit some records, so builder will reach limit
        builder.traceEnter(c1, m2, s1, 5*MS);
        builder.traceReturn(6*MS);
        builder.traceEnter(c1, m2, s1, 7*MS);
        builder.traceReturn(8*MS);
        builder.traceEnter(c1, m2, s1, 9*MS);
        builder.traceReturn(10*MS);

        // Return back to trace root frame
        builder.traceReturn(11*MS);

        // Check inner trace
        TraceRecord top = TestUtil.getField(builder, "ttop");
        assertEquals("Root record of a trace should have only one child.", 1, top.numChildren());
        Assert.assertEquals("Should record traceBegin and 3 full frames (3 records each)",
                1 + 3 * 3, output.size());

        output.clear();

        builder.traceReturn(16*MS);

        Assert.assertEquals("Should record 2 times traceBegin and 4 full frames (3 records each)",
                2 + 4 * 3, output.size());

        output.check(2, "flags", TraceMarker.OVERFLOW_FLAG);
    }


    @Test
    public void testTraceWithMultipleBeginFlags() throws Exception {
        spy.setTracerMinTraceTime(0);
        ZorkaLogConfig.setTracerLevel(0); // TODO check why ZorkaLog objects are not constructed correctly in this test

        builder.traceEnter(c1, m1, s1, 1*MS);
        builder.traceBegin(t1, 2*MS);
        builder.traceBegin(t2, 2*MS);
        builder.traceReturn(3*MS);

        Assert.assertEquals("Should record one traceBegin event and one full frame (3 records)", 1 + 3, output.size());
        output.check(0, "action", "traceBegin", "traceId", t1);
    }


    @Test
    public void testTraceWithTooManyReturns() throws Exception {
        spy.setTracerMinTraceTime(0);

        // Submit one frame
        builder.traceEnter(c1, m1, s1, 1*MS);
        builder.traceBegin(t1, 2*MS);
        builder.traceReturn(3*MS);

        // Malicious traceReturn event
        builder.traceReturn(4*MS);

        // Submit another frame
        builder.traceEnter(c1, m1, s1, 5*MS);
        builder.traceBegin(t1, 6*MS);
        builder.traceReturn(7*MS);

        Assert.assertEquals("Should record one traceBegin event and one full frame (3 records)", 2 * (1 + 3), output.size());
    }


    @Test
    public void testSingleTraceWithMultipleEmbeddedTracesInside() throws Exception {
        spy.setTracerMinTraceTime(0);

        // Submit one frame
        builder.traceEnter(c1, m1, s1, 1*MS);
        builder.traceBegin(t1, 2*MS);

        // Start subsequent trace
        builder.traceEnter(c1, m2, s1, 3*MS);
        builder.traceBegin(t2, 4*MS);
        builder.traceReturn(5*MS);

        // Single trace should appear on output
        Assert.assertEquals(4, output.size());
        output.clear();

        // Start subsequent trace
        builder.traceEnter(c1, m2, s1, 6*MS);
        builder.traceBegin(t2, 7*MS);
        builder.traceReturn(8*MS);

        Assert.assertEquals(4, output.size());
        output.clear();

        // Return from main frame
        builder.traceReturn(9*MS);

        Assert.assertEquals(3+3*3, output.size());
    }

}
