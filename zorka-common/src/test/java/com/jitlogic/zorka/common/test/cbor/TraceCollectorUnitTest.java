package com.jitlogic.zorka.common.test.cbor;

import com.jitlogic.zorka.common.collector.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import com.jitlogic.zorka.common.util.Base64;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

import static com.jitlogic.zorka.common.test.support.TraceBuildUtil.*;

public class TraceCollectorUnitTest {

    private SymbolRegistry registry;
    private MemoryChunkStore store;
    private Collector collector;

    @Before
    public void initOutput() {
        registry = new SymbolRegistry();
        store = new MemoryChunkStore();
        collector = new Collector(1, registry, store, false);
    }

    private byte[] agd1 = trace(
        sref(41,"component"),
        sref(42,"mydb.PStatement"),
        sref(43,"execute"),
        sref(44,"V()"),
        sref(45,"db"),
        sref(46, "invoke"),
        sref(47, "myweb.Valve"),
        sref(48, "http"),
        sref(49, "my.Class"),
        sref(50, "myMethod"),
        mref(11,42,43,44),
        mref(12,47,46,44),
        mref(13,49,50,44));

    private byte[] trc1 = trace(
        start(0, 100, 11,
            begin(1000, 45, 0x1234567812345001L, 0),
            attr(41,"db"),
            end(0, 200, 2, 0)));

    @Test
    public void testCollectRetrieveTrace() {
        collector.handleAgentData("1234", true, agd1);
        collector.handleTraceData("1234", "9234567812345001", 0, trc1);
        assertEquals(1, store.size());
        TraceDataExtractor tex = new TraceDataExtractor(registry);
        TraceChunkData tcd = store.get(0);
        assertNotEquals(Base64.encode(trc1, false), Base64.encode(tcd.getTraceData(), false));
        TraceDataResult tdr = tex.extract(Collections.singletonList(tcd));
        assertNotNull(tdr);
        assertEquals("mydb.PStatement.execute()", tdr.getMethod());
        assertEquals("db", tdr.getAttr("component"));
    }

    private byte[] trc2 = trace(
        start(0, 100, 12,
            begin(1000, 48, 0x1234567812345002L, 0),
            attr(41, "http"),
            start(0, 200, 11,
                begin(1001, 45, 0x1234567812345003L, 0),
                attr(41, "db"),
                end(0, 300, 2, 0)),
            start(0, 310, 13,
                end(0, 350, 1, 0)),
            end(0, 400, 5, 0)));

    @Test
    public void testCollectRetrieveEmbeddedTrace() {
        collector.handleAgentData("1234", true, agd1);
        collector.handleTraceData("1234", "9234567812345001", 0, trc2);
        assertEquals(2, store.size());
        TraceDataExtractor tex = new TraceDataExtractor(registry);

        TraceChunkData tcd0 = store.get(0);
        assertEquals("execute", tcd0.getMethod());

        assertEquals(1, tcd0.getRecs());

        TraceDataResult tdr1 = tex.extract(Collections.singletonList(tcd0));
        assertEquals("mydb.PStatement.execute()", tdr1.getMethod());
        assertNull(tdr1.getChildren());

        TraceChunkData tcd1 = store.get(1);
        assertEquals("invoke", tcd1.getMethod());

        assertEquals(0, tcd1.getErrors());
        assertEquals(3, tcd1.getRecs());

        TraceDataResult tdr2 = tex.extract(Collections.singletonList(tcd1));
        assertEquals("myweb.Valve.invoke()", tdr2.getMethod());
        assertNotNull(tdr2.getChildren());

        //assertEquals(1, tdr2.getChildren().size());

    }

}
