/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.test.tracer;

import com.jitlogic.zorka.tracer.SymbolRegistry;
import com.jitlogic.zorka.tracer.TraceBuilder;
import com.jitlogic.zorka.tracer.TraceElement;
import com.jitlogic.zorka.tracer.WrappedException;
import com.jitlogic.zorka.test.spy.support.TestTracer;

import com.jitlogic.zorka.util.ZorkaAsyncThread;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class TraceBuilderUnitTest {

    private TestTracer output = new TestTracer();
    private SymbolRegistry symbols = new SymbolRegistry();

    private TraceBuilder builder = new TraceBuilder(
        new ZorkaAsyncThread<TraceElement>("test") {
            @Override public void submit(TraceElement obj) { obj.traverse(output); }
            @Override protected void process(TraceElement obj) {  }
        });

    private static final int MS = 1000000;

    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int m2 = symbols.symbolId("otherMethod");
    private int s1 = symbols.symbolId("()V");
    private int t1 = symbols.symbolId("TRACE1");
    private int a1 = symbols.symbolId("ATTR1");
    private int a2 = symbols.symbolId("ATTR2");


    @Test
    public void testStrayTraceFragment() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceReturn(200*MS);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleTraceWithOneShortElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1);
        builder.traceReturn(100*MS+100);

        assertEquals("Nothing should be sent to output", 0, output.getData().size());
    }


    @Test
    public void testSingleOneElementTrace() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1);
        builder.traceReturn(200*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceReturn"),
                          output.listAttr("action"));
    }


    @Test
    public void testSingleTraceWithOneChildElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1);
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
        builder.traceBegin(t1);
        builder.traceError(new WrappedException(new Exception("oja!")), 200*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "traceError"),
                          output.listAttr("action"));
    }


    @Test
    public void testTraceWithShortErrorChildElement() throws Exception {
        builder.traceEnter(c1, m1, s1, 100*MS);
        builder.traceBegin(t1);
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
        builder.traceBegin(t1);
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
        builder.traceBegin(t1);
        builder.newAttr(a1, "some val");
        builder.newAttr(a2, "other val");
        builder.traceReturn(110*MS);

        Assert.assertEquals("Output actions mismatch.",
            Arrays.asList("traceBegin", "traceEnter", "traceStats", "newAttr", "newAttr", "traceReturn"),
            output.listAttr("action"));
    }
}
