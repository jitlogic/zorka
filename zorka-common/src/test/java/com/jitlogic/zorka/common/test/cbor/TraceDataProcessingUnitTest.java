package com.jitlogic.zorka.common.test.cbor;

import com.jitlogic.zorka.common.cbor.*;
import com.jitlogic.zorka.common.test.support.TapSymbolsScanningVisitor;
import com.jitlogic.zorka.common.test.support.TapTraceDataProcessor;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TraceDataProcessingUnitTest {

    @Test
    public void testEncodeDecodeAgentData() {
        CborDataWriter cdw = new CborDataWriter(512, 512);
        TraceDataWriter tdw = new TraceDataWriter(cdw);
        tdw.stringRef(1, "com.myapp.MyClass");
        tdw.stringRef(2, "myMethod");
        tdw.stringRef(3, "()V");
        tdw.methodRef(4, 1, 2, 3);

        CborDataReader cdr = new CborDataReader(cdw.toByteArray());
        TapTraceDataProcessor tap = new TapTraceDataProcessor();
        TraceDataReader tdr = new TraceDataReader(cdr, tap);
        tdr.run();

        assertTrue(tap.has("stringRef|1|com.myapp.MyClass"));
        assertTrue(tap.has("stringRef|2|myMethod"));
        assertTrue(tap.has("stringRef|3|()V"));
        assertTrue(tap.has("methodRef|4|1|2|3"));
    }

    @Test
    public void testEncodeDecodeTraceData() {
        CborDataWriter cdw = new CborDataWriter(512, 512);
        TraceDataWriter tdw = new TraceDataWriter(cdw);

        tdw.traceStart(0, 100L, 1);
        tdw.traceBegin(1, 2, 3, 4);
        tdw.traceAttr(1, "foo");
        tdw.traceAttr(2, 3, "bar");
        tdw.exception(1, 2, "test", 4, Arrays.asList(new int[]{1,2,3,4},new int[]{5,6,7,8}),null);
        tdw.exceptionRef(42);
        tdw.traceEnd(0, 200L, 1, 0);

        CborDataReader cdr = new CborDataReader(cdw.toByteArray());
        TapTraceDataProcessor tap = new TapTraceDataProcessor();
        TraceDataReader tdr = new TraceDataReader(cdr, tap);
        tdr.run();

        assertTrue(tap.has("traceStart|0|100|1"));
        assertTrue(tap.has("traceBegin|1|2|3|4"));
        assertTrue(tap.has("traceAttr|1|foo"));
        assertTrue(tap.has("traceAttr|2|3|bar"));
        assertTrue(tap.has("exception|1|2|test|4|[1,2,3,4][5,6,7,8]|null"));
        assertTrue(tap.has("exceptionRef|42"));
        assertTrue(tap.has("traceEnd|200|1|0"));
    }

    @Test
    public void testSymbolScanningVisitor() {
        CborDataWriter cdw = new CborDataWriter(512,512);
        TraceDataWriter tdw = new TraceDataWriter(cdw);
        TapSymbolsScanningVisitor visitor = new TapSymbolsScanningVisitor(2);
        TraceDataScanner ssp = new TraceDataScanner(visitor, tdw);

        ssp.traceStart(0, 100L, 10);
        assertFalse(visitor.hasSymbol(10));
        assertTrue(visitor.hasMethod(10));

        ssp.traceBegin(11, 12, 13, 14);
        assertFalse(visitor.hasSymbol(11));
        assertTrue(visitor.hasSymbol(12));
        assertFalse(visitor.hasSymbol(13));
        assertFalse(visitor.hasSymbol(14));

        ssp.traceAttr(31, "foo");
        assertTrue(visitor.hasSymbol(31));
        assertFalse(visitor.hasMethod(31));

        ssp.traceAttr(32, 33, "bar");
        assertTrue(visitor.hasSymbol(32));
        assertTrue(visitor.hasSymbol(33));

        ssp.exception(41, 42, "test", 84, Arrays.asList(new int[]{43,44,45,46},new int[]{47,48,49,50}),null);
        assertFalse(visitor.hasSymbol(41));
        assertTrue(visitor.hasSymbol(42));
        assertFalse(visitor.hasSymbol(84));
        assertTrue(visitor.hasSymbol(43));
        assertTrue(visitor.hasSymbol(44));
        assertTrue(visitor.hasSymbol(45));
        assertFalse(visitor.hasSymbol(46));
        assertTrue(visitor.hasSymbol(47));
        assertTrue(visitor.hasSymbol(48));
        assertTrue(visitor.hasSymbol(49));
        assertFalse(visitor.hasSymbol(50));

        ssp.exceptionRef(51);

        ssp.traceEnd(0, 200L, 61, 0);
        assertFalse(visitor.hasSymbol(61));

        CborDataReader cdr = new CborDataReader(cdw.toByteArray());
        TapTraceDataProcessor tap = new TapTraceDataProcessor();
        TraceDataReader tdr = new TraceDataReader(cdr, tap);
        tdr.run();

        assertTrue(tap.has("traceStart|0|100|12"));
        assertTrue(tap.has("traceBegin|11|14|13|14"));
        assertTrue(tap.has("traceAttr|33|foo"));
        assertTrue(tap.has("traceAttr|34|35|bar"));
        assertTrue(tap.has("exception|41|44|test|84|[45,46,47,46][49,50,51,50]|null"));
        assertTrue(tap.has("exceptionRef|51"));
        assertTrue(tap.has("traceEnd|200|61|0"));
    }
}
