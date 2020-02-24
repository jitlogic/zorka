package com.jitlogic.zorka.common.test.collector;

import com.jitlogic.zorka.common.collector.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;

import com.jitlogic.zorka.common.util.Base64;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.jitlogic.zorka.common.collector.TraceDataExtractingProcessor.extractTrace;
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
        collector = new Collector(store, false);
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
        sref(51, "query"),
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
        assertEquals(1, store.length());
        TraceChunkData tcd = store.get(0);
        assertNotEquals(Base64.encode(trc1, false), Base64.encode(tcd.getTraceData(), false));
        TraceDataResult tdr = extractTrace(Collections.singletonList(tcd));
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
        assertEquals(2, store.length());


        TraceChunkData tcd0 = store.get(0);
        assertEquals("execute", tcd0.getMethod());

        assertEquals(1, tcd0.getRecs());

        TraceDataResult tdr1 = extractTrace(Collections.singletonList(tcd0));
        assertEquals("mydb.PStatement.execute()", tdr1.getMethod());
        assertNull(tdr1.getChildren());

        TraceChunkData tcd1 = store.get(1);
        assertEquals("invoke", tcd1.getMethod());

        assertEquals(0, tcd1.getErrors());
        assertEquals(3, tcd1.getRecs());

        TraceDataResult tdr2 = extractTrace(Collections.singletonList(tcd1));
        assertEquals("myweb.Valve.invoke()", tdr2.getMethod());
        assertNotNull(tdr2.getChildren());

        //assertEquals(1, tdr2.getChildren().size());

    }

    private byte[] trc(long sid, long tst, long dur, String attrc, String attrq) {
        return trace(
            start(0, tst, 11,
                begin(1000, 45, sid, 0),
                attr(41,attrc), attr(51, attrq),
                end(0, tst+dur, 2, 0)));
    }

    @Test
    public void testCollectSearchTracesSortOrder() {
        collector.handleAgentData("1234", true, agd1);
        for (int i = 0; i < 256; i++) {
            collector.handleTraceData("1234", String.format("%016x", i), 0,
                trc(i, i, 16-(i%16), "foo", String.format("bar%01x", (i%16))));
        }
        assertEquals(256, store.length());

        TraceChunkSearchQuery q = new TraceChunkSearchQuery();

        // Check limits and offsets
        q.setLimit(1000);
        List<TraceChunkData> l1 = store.search(q);

        assertEquals(256, l1.size());
        for (int i = 0; i < l1.size()-1; i++) {
            assertTrue(l1.get(i).getTstamp() <= l1.get(i+1).getTstamp());
        }

        q.setOffset(100);
        List<TraceChunkData> l2 = store.search(q);
        assertEquals(156, l2.size());

        q.setSortByDuration(true);
        List<TraceChunkData> l3 = store.search(q);
        assertEquals(156, l3.size());
        for (TraceChunkData c : l3) assertTrue(c.getDuration() >= 7);
    }

    @Test
    public void testCollectSearchTracesFiltering() {
        collector.handleAgentData("1234", true, agd1);
        for (int i = 0; i < 256; i++) {
            collector.handleTraceData("1234", String.format("%016x", i), 0,
                trc(i, i, 16-(i%16), "foo", String.format("bar%01x", (i%16))));
        }
        assertEquals(256, store.length());

        TraceChunkSearchQuery q = new TraceChunkSearchQuery();
        q.setLimit(1000);

        q.setMinDuration(9);
        List<TraceChunkData> l1 = store.search(q);
        assertEquals(128, l1.size());
        for (TraceChunkData c : l1)assertTrue(c.getDuration() > 8);

        q.setMinDuration(0);
        q.setText("barf");
        List<TraceChunkData> l2 = store.search(q);
        assertEquals(16, l2.size());
        for (TraceChunkData c : l2) assertEquals("barf", c.getAttr("query"));

        q.setText(null);
        q.withAttr("query", "bard");
        List<TraceChunkData> l3 = store.search(q);
        assertEquals(16, l3.size());
        for (TraceChunkData c : l3) assertEquals("bard", c.getAttr("query"));
    }
}
