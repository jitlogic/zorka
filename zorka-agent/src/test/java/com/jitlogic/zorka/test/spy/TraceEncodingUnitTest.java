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

package com.jitlogic.zorka.test.spy;

import com.jitlogic.zorka.spy.*;

import com.jitlogic.zorka.test.spy.support.TestTracer;

import org.junit.Assert;
import org.junit.Test;

public class TraceEncodingUnitTest {

    private SymbolRegistry symbols = new SymbolRegistry();


    private ByteBuffer buf = new ByteBuffer();;
    private SimpleTraceFormat encoder = new SimpleTraceFormat(buf);;
    private TestTracer output = new TestTracer();


    private int t1 = symbols.symbolId("some.trace");
    private int c1 = symbols.symbolId("some.Class");
    private int m1 = symbols.symbolId("someMethod");
    private int s1 = symbols.symbolId("()V");
    private int e1 = symbols.symbolId("some.Exception");
    private int a1 = symbols.symbolId("someAttr");


    private void decode() {
        new SimpleTraceFormat(buf.getContent()).decode(output);
    }


    @Test
    public void testSymbolCmd() {
        encoder.newSymbol(c1, "some.Class");
        decode();
        output.check(0, "action", "newSymbol", "symbolId", c1, "symbolName", "some.Class");
    }


    @Test
    public void testTraceBeginCmd() {
        encoder.traceBegin(t1);
        decode();
        output.check(0, "action", "traceBegin", "traceId", t1);
    }


    @Test
    public void testTraceEnterCmd() {
        encoder.traceEnter(c1, m1, s1, 100);
        decode();
        output.check(0, "action", "traceEnter", "classId", c1, "methodId", m1, "signatureId", s1, "tstamp", 100L);
    }


    @Test
    public void testTraceReturnCmd() {
        encoder.traceReturn(200);
        decode();
        output.check(0, "action", "traceReturn", "tstamp", 200L);
    }


    @Test
    public void traceErrorCmd() {
        TracedException e = new SymbolicException(e1, "oja!");
        encoder.traceError(e, 200);
        decode();
        output.check(0, "action", "traceError", "tstamp", 200L, "exception", e);
    }

    @Test
    public void traceStatsCmd() {
        encoder.traceStats(100, 50);
        decode();
        output.check(0, "action", "traceStats", "calls", 100L, "errors", 50L);
    }


    @Test
    public void traceStringAttr() {
        encoder.newAttr(a1, "oja!");
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "val", "oja!");
    }


    @Test
    public void traceLongAttr() {
        encoder.newAttr(a1, 100L);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "val", 100L);
    }


    @Test
    public void testIntegerAttr() {
        encoder.newAttr(a1, 100);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "val", 100);
    }


    @Test
    public void testByteAttr() {
        encoder.newAttr(a1, (byte)10);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "val", (byte)10);
    }


    @Test
    public void testShortAttr() {
        encoder.newAttr(a1, (short)200);
        decode();
        output.check(0, "action", "newAttr", "attrId", a1, "val", (short)200);
    }


    @Test
    public void testSimpleTraceDecodeEncode() {
        encoder.traceBegin(t1);
        encoder.traceEnter(c1, m1, s1, 100L);
        encoder.traceStats(10, 5);
        encoder.newAttr(a1, "http://some/ctx");
        encoder.traceReturn(200L);

        decode();

        Assert.assertEquals("Should read 5 records.", 5, output.getData().size());
    }
}
