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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.st.STraceBufChunk;
import com.jitlogic.zorka.core.spy.st.STraceBufManager;
import com.jitlogic.zorka.core.test.spy.cbor.TestTraceBufOutput;
import com.jitlogic.zorka.core.test.spy.cbor.TestTraceRecorder;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.util.ZorkaUnsafe;
import org.junit.After;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.jitlogic.zorka.core.test.spy.cbor.TraceCborUtils.chunksCount;
import static com.jitlogic.zorka.core.test.spy.cbor.TraceCborUtils.decodeTrace;
import static com.jitlogic.zorka.core.test.spy.cbor.TraceCborUtils.mkString;
import static com.jitlogic.zorka.core.util.ZorkaUnsafe.BYTE_ARRAY_OFFS;
import static org.junit.Assert.*;

public class STraceHandlerUnitTest extends ZorkaFixture {

    private SymbolRegistry symbols = new SymbolRegistry();


    @After
    public void tearDown() {
        tracer.setTracerMinMethodTime(250000);
        tracer.setTracerMinTraceTime(50);
    }

    private STraceBufManager bm = new STraceBufManager(128, 4);


    private TestTraceBufOutput o = new TestTraceBufOutput();

    private TestTraceRecorder r = new TestTraceRecorder(bm,symbols,o);

    private static Object l(Object...args) {
        return Arrays.asList(args);
    }

    private static Map m(Object...args) {
        return ZorkaUtil.map(args);
    }


    @Test
    public void testStrayShortTraceFragment(){
        r.t = 1; r.traceEnter(10);
        r.t = 3; r.traceReturn();

        assertNull(o.getChunks());

        assertEquals(1, bm.getNGets());
        assertEquals(0, bm.getNputs());
    }

    @Test
    public void testStrayLongTraceFragment(){
        r.t = 1; r.traceEnter(10);
        r.t = 9; r.traceReturn();

        assertNull(o.getChunks());

        assertEquals(1, bm.getNGets());
        assertEquals(0, bm.getNputs());
    }

    @Test
    public void testTraceSingleCallWithSingleMethod() throws Exception {

        r.setMinimumTraceTime(0);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 9; r.traceReturn();

        assertEquals(1, chunksCount(o.getChunks()));

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11, "trace", 1)), decodeTrace(o.getChunks()));
    }

    @Test
    public void testTraceSingleCallWithTwoMethods() throws Exception {
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 8; r.traceReturn();
        r.t = 9; r.traceReturn();


        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 8L, "calls", 1L))
        ), decodeTrace(o.getChunks()));
    }


    @Test
    public void testTraceSingleCallWithSkippedMethod() throws Exception {
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 6; r.traceReturn();
        r.t = 9; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11, "trace", 1)
        ), decodeTrace(o.getChunks()));
    }

    @Test
    public void testTraceSampleMethodCallsWithSomeAttributes() throws Exception {
        r.setMinimumTraceTime(0);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.newAttr(-1, 99, "OJAAA!");
        r.t = 9; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "attrs", m(99, "OJAAA!")),
            decodeTrace(o.getChunks()));
    }

    @Test
    public void testTraceSampleMethodCallWithAttrsBelowTraceBegin() throws Exception {
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 3; r.newAttr(-1, 99, "OJAAA!");
        r.t = 8; r.traceReturn();
        r.t = 9; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 8L,
                            "calls", 1L, "attrs", m(99, "OJAAA!")))
        ), decodeTrace(o.getChunks()));
    }


    @Test
    public void testIfAttrsForceMethodNotToBeExcludedInTraceTransitiveness() throws Exception {
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 4; r.traceEnter(14);
        r.t = 4; r.newAttr(-1, 99, "OJAAA!");
        r.t = 9; r.traceReturn();
        r.t = 10; r.traceReturn();
        r.t = 11; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
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
                se.getLineNumber()));
        }
        return m(
            "_", "E",
            "id", System.identityHashCode(e),
            "class", symbols.symbolId(e.getClass().getName()),
            "message", e.getMessage(), "stack", stack);
    }


    @Test
    public void testTraceException() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 8; r.traceError(e);

        assertEquals(
            m("_", "T", "method", 10L, "tstart", 1L, "tstop", 8L, "calls", 1L,
                "begin", m("_", "B", "clock", 11, "trace", 1),
                "error", errorToMap(e)),
            decodeTrace(o.getChunks()));
    }


    @Test
    public void testTraceExceptionInRecursiveMethodWithTransitiveness() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 4; r.traceEnter(14);
        r.t = 9; r.traceError(e);
        r.t = 10; r.traceReturn();
        r.t = 11; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e)))))),
            decodeTrace(o.getChunks()));
    }


    @Test
    public void testTraceExceptionInRecursiveMethodWithTransitivenessWithAttr() throws Exception {
        Exception e = new RuntimeException("test");
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 4; r.traceEnter(14);
        r.t = 4; r.newAttr(-1, 99, "OJAAA!");
        r.t = 9; r.traceError(e);
        r.t = 10; r.traceReturn();
        r.t = 11; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e), "attrs", m(99, "OJAAA!")))))
        ), decodeTrace(o.getChunks()));
    }


    @Test
    public void testTraceExceptionCompression() throws Exception {
        Exception e = new RuntimeException("test");
        int id = System.identityHashCode(e);
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 4; r.traceEnter(14);
        r.t = 9; r.traceError(e);
        r.t = 10; r.traceError(e);
        r.t = 11; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "error", m("_", "E", "id", id),
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error", errorToMap(e)))))
        ), decodeTrace(o.getChunks()));
    }


    @Test
    public void testTraceExceptionCompressCause() throws Exception {
        Exception e1 = new RuntimeException("test1");
        Exception e2 = new RuntimeException("test2", e1);

        int i1 = System.identityHashCode(e1);
        int i2 = System.identityHashCode(e2);

        Map em1 = errorToMap(e1);
        Map em2 = errorToMap(e2);

        em2.put("cause", m("_", "E", "id", i1));

        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.traceEnter(12);
        r.t = 4; r.traceEnter(14);
        r.t = 9; r.traceError(e1);
        r.t = 10; r.traceError(e2);
        r.t = 11; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 11L, "calls", 3L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
            "children", l(m("_", "T", "method", 12L, "tstart", 3L, "tstop", 10L, "calls", 2L,
                "error", em2,
                "children", l(m("_", "T", "method", 14L, "tstart", 4L, "tstop", 9L, "calls", 1L,
                    "error",em1))))
        ), decodeTrace(o.getChunks()));
    }

    @Test
    public void testDisableEnableTracer() throws Exception {
        r.setMinimumTraceTime(5);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 3; r.disable();;
        r.t = 3; r.traceEnter(12);
        r.t = 8; r.traceReturn();
        r.t = 8; r.enable();
        r.t = 9; r.traceReturn();

        assertEquals(1, chunksCount(o.getChunks()));

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 1L,
            "begin", m("_", "B", "clock", 11, "trace", 1)),
            decodeTrace(o.getChunks()));

    }


    @Test
    public void testSimpleTraceBufOverflowAndReturnWithMethodHalfDrop() throws Exception {
        r.setMinimumTraceTime(5);
        String s = mkString(104);

        r.t = 1; r.traceEnter(10);
        r.t = 2; r.traceBegin(1, 11, 0);
        r.t = 2;
        r.t = 3; r.traceEnter(12);
        assertEquals(1, chunksCount((STraceBufChunk)ObjectInspector.getField(r, "chunk")));
        r.newAttr(-1, 1, s);
        assertEquals(2, chunksCount((STraceBufChunk)ObjectInspector.getField(r, "chunk")));
        r.t = 6; r.traceReturn();
        r.t = 9; r.traceReturn();

        assertEquals(m(
            "_", "T", "method", 10L, "tstart", 1L, "tstop", 9L, "calls", 2L,
            "begin", m("_", "B", "clock", 11, "trace", 1),
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


    //@Test @Ignore
    public void testSlowLongWriter() {
        byte[] b = new byte[4*1024*1024];
        for (long l = 0; l < 20*1024; l++) {
            slowLongWriter(b, l);
        }
    }

    public void fastLongWriter(byte[] b, long v) {
        for (int pos = 0; pos < b.length; pos += 8) {
            ZorkaUnsafe.UNSAFE.putLong(b, pos+BYTE_ARRAY_OFFS, v);
        }
    }


    //@Test @Ignore
    public void testFastLongWriter() {
        byte[] b = new byte[4*1024*1024];
        for (long l = 0; l < 20*1024; l++) {
            fastLongWriter(b, l);
        }
    }


    public void slowIntWriter(byte[] b, int v) {
        for (int pos = 0; pos < b.length; pos += 4) {
            b[pos] = (byte)((v>>24)&0xff);
            b[pos+1] = (byte)((v>>16)&0xff);
            b[pos+2] = (byte)((v>>8)&0xff);
            b[pos+3] = (byte)(v&0xff);
        }
    }


    //@Test @Ignore
    public void testSlowIntWriter() {
        byte[] b = new byte[4*1024*1024];
        for (int l = 0; l < 20*1024; l++) {
            slowIntWriter(b, l);
        }
    }


    public void fastIntWriter(byte[] b, int v) {
        for (int pos = 0; pos < b.length; pos += 4) {
            ZorkaUnsafe.UNSAFE.putInt(b, pos+BYTE_ARRAY_OFFS, v);
        }
    }


    //@Test @Ignore
    public void testFastIntWriter() {
        byte[] b = new byte[4*1024*1024];
        for (int l = 0; l < 20*1024; l++) {
            fastIntWriter(b, l);
        }
    }


    public void slowShortWriter(byte[] b, short v) {
        for (int pos = 0; pos < b.length; pos += 2) {
            b[pos] = (byte)((v>>8)&0xff);
            b[pos+1] = (byte)(v&0xff);
        }
    }


    //@Test @Ignore
    public void testSlowShortWriter() {
        byte[] b = new byte[4*1024*1024];
        for (short l = 0; l < 20*1024; l++) {
            slowShortWriter(b, l);
        }
    }


    public void fastShortWriter(byte[] b, short v) {
        for (int pos = 0; pos < b.length; pos += 2) {
            ZorkaUnsafe.UNSAFE.putShort(b, pos+BYTE_ARRAY_OFFS, v);
        }
    }


    //@Test @Ignore
    public void testFastShortWriter() {
        byte[] b = new byte[4*1024*1024];
        for (short l = 0; l < 20*1024; l++) {
            fastShortWriter(b, l);
        }
    }

    //@Test @Ignore
    public void testEndianness() {
        byte[] b = new byte[8];
        long v = 0x0102030405060708L;

        ZorkaUnsafe.UNSAFE.putLong(b, BYTE_ARRAY_OFFS, v);

        System.out.println("Current order (LE):");

        for (int i = 0; i < b.length; i++) {
            System.out.println("b[" + i + "]=" + b[i]);
        }

        b[0] = (byte)((v>>56)&0xff);
        b[1] = (byte)((v>>48)&0xff);
        b[2] = (byte)((v>>40)&0xff);
        b[3] = (byte)((v>>32)&0xff);
        b[4] = (byte)((v>>24)&0xff);
        b[5] = (byte)((v>>16)&0xff);
        b[6] = (byte)((v>>8)&0xff);
        b[7] = (byte)(v&0xff);

        System.out.println("Reference order (BE):");

        for (int i = 0; i < b.length; i++) {
            System.out.println("b[" + i + "]=" + b[i]);
        }

    }

}
