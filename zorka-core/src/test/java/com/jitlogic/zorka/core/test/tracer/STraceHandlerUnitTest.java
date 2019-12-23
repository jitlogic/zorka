/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.tracer;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.ltracer.TraceHandler;
import com.jitlogic.zorka.core.spy.stracer.STraceBufChunk;
import com.jitlogic.zorka.core.spy.stracer.STraceBufManager;
import com.jitlogic.zorka.core.test.spy.support.cbor.TestTraceBufOutput;
import com.jitlogic.zorka.core.test.spy.support.cbor.TestSTraceHandler;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.util.ZorkaUnsafe;
import org.junit.After;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.test.spy.support.cbor.STTrcTestUtils.chunksCount;
import static com.jitlogic.zorka.core.test.spy.support.cbor.STTrcTestUtils.decodeTrace;
import static com.jitlogic.zorka.core.test.spy.support.cbor.STTrcTestUtils.mkString;
import static org.junit.Assert.*;

public class STraceHandlerUnitTest extends ZorkaFixture {

    private SymbolRegistry symbols = new SymbolRegistry();

    private STraceBufManager bm = new STraceBufManager(128, 4);
    private TestTraceBufOutput o = new TestTraceBufOutput();
    private TestSTraceHandler r;

    @Before
    public void setUp() {
        TraceHandler.setMinMethodTime(4 * 65536);
        r = new TestSTraceHandler(bm,symbols,o);
    }

    @After
    public void tearDown() {
        tracer.setTracerMinMethodTime(250000);
        tracer.setTracerMinTraceTime(50);
    }

    private static Object l(Object...args) {
        return Arrays.asList(args);
    }

    private static Map m(Object...args) {
        return ZorkaUtil.map(args);
    }


    @Test
    public void testStrayShortTraceFragment(){
        r.traceEnter(10, 1<<16);
        r.traceReturn(3<<16);

        assertNull(o.getChunks());

        assertEquals(1, bm.getNGets());
        assertEquals(0, bm.getNputs());
    }

    @Test
    public void testStrayLongTraceFragment(){
        r.traceEnter(10, 1<<16);
        r.traceReturn(9<<16);

        assertNull(o.getChunks());

        assertEquals(1, bm.getNGets());
        assertEquals(0, bm.getNputs());
    }

    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceSingleCallWithSingleMethod() throws Exception {

        r.setMinimumTraceTime(0);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceReturn(9<<16);

        assertEquals(1, chunksCount(o.getChunks()));

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11)), decodeTrace(o.getChunks()));
    }

    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceSingleCallWithTwoMethods() throws Exception {
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceReturn(8<<16);
        r.traceReturn(9<<16);


        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 8L, "calls", 1L))
        ), decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceSingleCallWithSkippedMethod() throws Exception {
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceReturn(6<<16);
        r.traceReturn(9<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11)
        ), decodeTrace(o.getChunks()));
    }

    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceSampleMethodCallsWithSomeAttributes() throws Exception {
        r.setMinimumTraceTime(0);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.newAttr(-1, 99, "OJAAA!");
        r.traceReturn(9<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11),
            "attrs", m(99, "OJAAA!")),
            decodeTrace(o.getChunks()));
    }

    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceSampleMethodCallWithAttrsBelowTraceBegin() throws Exception {
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.newAttr(-1, 99, "OJAAA!");
        r.traceReturn(8<<16);
        r.traceReturn(9<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 8L,
                            "calls", 1L, "attrs", m(99, "OJAAA!")))
        ), decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testIfAttrsForceMethodNotToBeExcludedInTraceTransitiveness() throws Exception {
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceEnter(14, 4<<16);
        r.newAttr(-1, 99, "OJAAA!");
        r.traceReturn(9<<16);
        r.traceReturn(10<<16);
        r.traceReturn(11<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "attrs", m(99, "OJAAA!")))))
        ), decodeTrace(o.getChunks()));
    }


    private Map errorToMap(Throwable e) {
        List<Object> stack = new ArrayList<Object>();
        for (int i = 0; i < e.getStackTrace().length; i++) {
            StackTraceElement se = e.getStackTrace()[i];
            stack.add(l(
                symbols.symbolId(se.getClassName()),
                symbols.symbolId(se.getMethodName()),
                symbols.symbolId(se.getFileName()),
                se.getLineNumber() > 0 ? se.getLineNumber() : 0));
        }
        return m(
            "_", "E",
            "id", System.identityHashCode(e),
            "class", symbols.symbolId(e.getClass().getName()),
            "message", e.getMessage(), "stack", stack);
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceException() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceError(e, 8<<16);

        assertEquals(
            m("_", "T", "method", 10L, "tstart", 1L, "tstop", 8L, "calls", 1L,
                "begin", m("_", "B", "clock", 11),
                "error", errorToMap(e)),
            decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceExceptionInRecursiveMethodWithTransitiveness() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceEnter(14, 4<<16);
        r.traceError(e, 9<<16);
        r.traceReturn(10<<16);
        r.traceReturn(11<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e)))))),
            decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceExceptionInRecursiveMethodWithTransitivenessWithAttr() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceEnter(14, 4<<16);
        r.newAttr(-1, 99, "OJAAA!");
        r.traceError(e, 9<<16);
        r.traceReturn(10<<16);
        r.traceReturn(11<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e), "attrs", m(99, "OJAAA!")))))
        ), decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceExceptionCompression() throws Exception {
        Exception e = new RuntimeException("test");
        int id = System.identityHashCode(e);
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceEnter(14, 4<<16);
        r.traceError(e, 9<<16);
        r.traceError(e, 10<<16);
        r.traceReturn(11<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "error", m("_", "E", "id", id),
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e)))))
        ), decodeTrace(o.getChunks()));
    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testTraceExceptionCompressCause() throws Exception {
        Exception e1 = new RuntimeException("test1");
        Exception e2 = new RuntimeException("test2", e1);

        int i1 = System.identityHashCode(e1);
        int i2 = System.identityHashCode(e2);

        Map em1 = errorToMap(e1);
        Map em2 = errorToMap(e2);

        // TODO em2.put("cause", m("_", "E", "id", i1));

        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        r.traceEnter(14, 4<<16);
        r.traceError(e1, 9<<16);
        r.traceError(e2, 10<<16);
        r.traceReturn(11<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "error", em2,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error",em1))))
        ), decodeTrace(o.getChunks()));
    }

    @Ignore("TBD fix streaming tracer later") @Test
    public void testDisableEnableTracer() throws Exception {
        r.setMinimumTraceTime(5);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.disable();
        r.traceEnter(12, 3<<16);
        r.traceReturn(8<<16);
        r.enable();
        r.traceReturn(9<<16);

        assertEquals(1, chunksCount(o.getChunks()));

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11)),
            decodeTrace(o.getChunks()));

    }


    @Ignore("TBD fix streaming tracer later") @Test
    public void testSimpleTraceBufOverflowAndReturnWithMethodHalfDrop() throws Exception {
        r.setMinimumTraceTime(5);
        String s = mkString(104);

        r.traceEnter(10, 1<<16);
        r.traceBegin(1, 11, 0);
        r.traceEnter(12, 3<<16);
        assertEquals(1, chunksCount((STraceBufChunk)ObjectInspector.getField(r, "chunk")));
        r.newAttr(-1, 1, s);
        assertEquals(2, chunksCount((STraceBufChunk)ObjectInspector.getField(r, "chunk")));
        r.traceReturn(6<<16);
        r.traceReturn(9<<16);

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11),
            "children", l(m("_", "T", "method", 12L, "calls", 1L, "tstart", 3L, "tstop", 6L, "attrs", m(1, s)))
        ), decodeTrace(o.getChunks()));
    }

    // TODO test embedded trace with and without automatic flush

    // TODO test forced trace submission;

    // TODO test forced trace flush;

    // TODO test test for embedded trace;

    // TODO test for forced flush of embedded trace;

    // TODO test for automatic flush of embedded trace;

    // TODO test for dropping partially sent trace;

    // TODO test for proper buffer queuing;

    // TODO test for automatic flush of queued buffers;

    // TODO test assigning attributes to specific traces;

    // TODO test for automatic flush after selected timeout - triggered from instrumentation;

    // TODO test for automatic flush after selected timeout - triggered from external thread;


    public void slowLongWriter(byte[] b, long v) {
        for (int pos = 0; pos < b.length; pos += 8) {
            b[pos] = (byte)((v>>56)&0xff);
            b[pos+1] = (byte)((v>>48)&0xff);
            b[pos+2] = (byte)((v>>40)&0xff);
            b[pos+3] = (byte)((v>>32)&0xff);
            b[pos+4] = (byte)((v>>24)&0xff);
            b[pos+5] = (byte)((v>>16)&0xff);
            b[pos+6] = (byte)((v>>8)&0xff);
            b[pos+7] = (byte)(v&0xff);
        }
    }

}
