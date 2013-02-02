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

package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.*;
import com.jitlogic.zorka.common.test.support.TestTracer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SimpleTraceFormatUnitTest {

    private SymbolRegistry symbols = new SymbolRegistry();
    private ByteBuffer buf = new ByteBuffer();;
    private SimplePerfDataFormat encoder = new SimplePerfDataFormat(buf);
    private TestTracer output = new TestTracer();


    private int t1 = symbols.symbolId("some.trace");
    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int s1 = symbols.symbolId("()V");
    private int e1 = symbols.symbolId("some.Exception");
    private int a1 = symbols.symbolId("someAttr");

    private void decode() {
        new SimplePerfDataFormat(buf.getContent()).decode(output);
    }

    @Test
    public void testSymbolCmd() {
        encoder.newSymbol(c1, "some.Class");
        decode();
        output.check(0, "action", "newSymbol", "symbolId", c1, "symbolName", "some.Class");
    }

    @Test
    public void testTraceBeginCmd() {
        encoder.traceBegin(t1, 100L, 8);
        decode();
        output.check(0, "action", "traceBegin", "traceId", t1, "clock", 100L, "flags", 8);
    }


    @Test
    public void testTraceEnterCmd() {
        encoder.traceEnter(c1, m1, s1, 100);
        decode();
        output.check(0, "action", "traceEnter", "classId", c1, "methodId", m1, "signatureId", s1, "tstamp", 0L);
    }


    @Test
    public void testTraceReturnCmd() {
        encoder.traceReturn(200);
        decode();
        output.check(0, "action", "traceReturn", "tstamp", 200L);
    }

    @Test
    public void testTraceBigLongNum() {
        long l = System.nanoTime();
        System.out.println(l);
        encoder.traceReturn(l);
        decode();
        output.check(0, "tstamp", l);
    }

    @Test
    public void traceErrorCmd() {
        SymbolicException e = new SymbolicException(e1, "oja!", new SymbolicStackElement[0], null);
        encoder.traceError(e, 200);
        decode();
        output.check(0, "action", "traceError", "tstamp", 200L, "exception", e);
    }

    @Test
    public void traceStatsCmd() {
        encoder.traceStats(100, 50, 1);
        decode();
        output.check(0, "action", "traceStats", "calls", 100L, "errors", 50L, "flags", 1);
    }


    @Test
    public void traceStringAttr() {
        encoder.newAttr(a1, "oja!");
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "attrVal", "oja!");
    }

    @Test
    public void traceLongAttr() {
        encoder.newAttr(a1, 100L);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "attrVal", 100L);
    }


    @Test
    public void testIntegerAttr() {
        encoder.newAttr(a1, 100);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "attrVal", 100);
    }


    @Test
    public void testByteAttr() {
        encoder.newAttr(a1, (byte)10);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "attrVal", (byte)10);
    }


    @Test
    public void testShortAttr() {
        encoder.newAttr(a1, (short)200);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "attrVal", (short)200);
    }

    @Test
    public void testSimpleTraceDecodeEncode() {
        encoder.traceBegin(t1, 500L, 2);
        encoder.traceEnter(c1, m1, s1, 100L);
        encoder.traceStats(10, 5, 1);
        encoder.newAttr(a1, "http://some/ctx");
        encoder.traceReturn(200L);

        decode();

        Assert.assertEquals("Should read 5 records.", 5, output.getData().size());
    }

    @Test
    public void testTraceEncodeDecodeRealException() {
        Exception ex = new Exception("oja!");
        SymbolicException e = new SymbolicException(ex, symbols, true);

        encoder.traceError(e, 200);
        decode();
        output.check(0, "action", "traceError", "tstamp", 200L, "exception", e);
    }

    @Test
    public void testTraceEncodeDecodeRealExceptionWithCause() {
        Exception ex1 = new Exception("oja!");
        Exception ex2 = new Exception("OJA!", ex1);
        SymbolicException e = new SymbolicException(ex2, symbols, true);

        encoder.traceError(e, 200);
        decode();
        output.check(0, "action", "traceError", "tstamp", 200L, "exception", e);
    }

    @Test
    public void testTraceEncodeDecodeLongVals() {
        List<Integer> c1 = Arrays.asList(new Integer[]{ 1, 2, 3, 4 });
        List<Long>    v1 = Arrays.asList(new Long[]{ 2L, 4L, 6L, 8L });

        encoder.longVals(100, 10, c1, v1);
        decode();
        output.check(0, "action", "longVals", "clock", 100L, "objId", 10);

        List<Integer>  c2 = (List<Integer>)output.get(0, "components");
        List<Long>     v2 = (List<Long>)output.get(0, "values");

        Assert.assertEquals(c1, c2);
        Assert.assertEquals(v1, v2);
    }

    @Test
    public void testTraceEncodeDecodeDoubleVals() {
        List<Integer>  c1 = Arrays.asList(new Integer[]{ 1, 2, 3, 4 });
        List<Double>   v1 = Arrays.asList(new Double[]{ 2.0, 4.0, 6.0, 8.0 });

        encoder.doubleVals(100, 10, c1, v1);
        decode();
        output.check(0, "action", "doubleVals", "clock", 100L, "objId", 10);

        int[]  c2 = (int[])output.get(0, "components");
        double[] v2 = (double[])output.get(0, "values");

        Assert.assertEquals(c1, c2);
        Assert.assertEquals(v1, v2);
    }
}
