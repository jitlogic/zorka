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
package com.jitlogic.zico.test;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceInfoSearchQuery;
import com.jitlogic.zico.core.model.TraceInfoSearchResult;
import com.jitlogic.zico.shared.data.TraceInfoSearchQueryProxy;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.tracedata.FressianTraceWriter;
import com.jitlogic.zorka.common.tracedata.MetricsRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceMarker;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoTraceOutput;

import org.junit.Before;
import org.junit.Test;

import static com.jitlogic.zico.test.support.ZicoTestUtil.*;
import static org.fest.reflect.core.Reflection.field;
import static org.fest.reflect.core.Reflection.method;
import static org.junit.Assert.*;


public class DataCollectionUnitTest extends ZicoFixture {

    private ZicoTraceOutput output;

    @Before
    public void setUpOutputAndCollector() throws Exception {

        symbols = new SymbolRegistry();
        metrics = new MetricsRegistry();

        output = new ZicoTraceOutput(
                new FressianTraceWriter(symbols, metrics),
                "127.0.0.1", 9640, "test", "aaa", 64, 8 * 1024 * 1024, 1, 250, 8, 30000);
    }


    private void submit(TraceRecord... recs) throws Exception {
        method("open").in(output).invoke();
        for (TraceRecord rec : recs) {
            output.submit(rec);
        }
        method("runCycle").in(output).invoke();
    }


    private int countTraces(String hostName) {
        HostStore store = hostStoreManager.getHost(hostName, false);

        assertNotNull("store should be existing", store);

        return store.countTraces();
    }


    @Test(timeout = 1000)
    public void testCollectSingleTraceRecord() throws Exception {
        submit(trace());

        assertEquals("One trace should be noticed.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectTwoTraceRecords() throws Exception {
        submit(trace());
        assertEquals("One trace should be noticed.", 1, countTraces("test"));
        submit(trace());
        assertEquals("Two traces should be noticed.", 2, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectThreeTraceRecordsInOneGo() throws Exception {
        submit(trace(), trace(), trace());
        assertEquals("Two traces should be noticed.", 3, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectThreeRecordsWithLimitPerPacket() throws Exception {
        field("packetSize").ofType(long.class).in(output).set(210L);
        submit(trace(), trace(), trace());
        assertEquals("Two traces should be noticed.", 2, countTraces("test"));
        submit();
        assertEquals("Two traces should be noticed.", 3, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectBrokenTraceCausingNPE() throws Exception {
        TraceRecord rec = trace();
        rec.setFlags(0);


        submit(rec);

        assertEquals("Trace will not reach store.", 0, countTraces("test"));

        submit(trace());
        assertEquals("TraceOutput should reconnect and send properly.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testSubmitCloseReopenReadTrace() throws Exception {
        submit(trace());
        assertEquals("One trace should be noticed.", 1, countTraces("test"));

        hostStoreManager.close();
        assertEquals("One trace should be still there.", 1, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testSubmitAndSearchSingleRecordWithoutCriteria() throws Exception {
        submit(trace(kv("SQL", "select count(1) from HOSTS")));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 0L, null));

        assertEquals(1, result.getResults().size());

        TraceInfo ti = result.getResults().get(0);
        assertEquals(1, ti.getAttributes().size());
        assertEquals("SQL", ti.getAttributes().get(0).getKey());
        assertTrue(ti.getDescription().startsWith("MY_TRACE"));
    }


    @Test(timeout = 1000)
    public void testSubmitMoreRecordsAndSearchWithSimpleCriteria() throws Exception {
        submit(trace(kv("SQL", "select count(*) from HOSTS")), trace(kv("SQL", "select count(1) from TRACES")));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 0L, "TRACES"));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitTwoRecordsAndFilterByTime() throws Exception {
        TraceRecord t1 = trace(); t1.setTime(500);
        TraceRecord t2 = trace(); t2.setTime(1500);
        submit(t1, t2);
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, null, 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitTwoRecordsAndFilterByTraceType() throws Exception {
        submit(traceP("HTTP", rClass(), rMethod(), rSignature(), 100),
                traceP("EJB", rClass(), rMethod(), rSignature(), 200));
        TraceInfoSearchResult result = traceDataService.searchTraces(tiq("test", 0, "EJB", 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterByMethodErrorMarker() throws Exception {
        TraceRecord t1 = trace(); t1.getMarker().setFlags(TraceMarker.ERROR_MARK);
        submit(t1, trace());
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQueryProxy.ERRORS_ONLY, null, 1000L, null));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterByMethodErrorObject() throws Exception {
        TraceRecord t1 = trace(); t1.setException(boo());
        submit(t1, trace());
        TraceInfoSearchQuery query = tiq("test", TraceInfoSearchQueryProxy.ERRORS_ONLY, null, 1000L, null);
        TraceInfoSearchResult result = traceDataService.searchTraces(query);
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterUsingEqlQuery() throws Exception {
        submit(trace(), trace(kv("SQL", "select 1")));
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQueryProxy.EQL_QUERY, null, 1000L, "SQL != null"));
        assertEquals(1, result.getResults().size());
    }


    @Test
    public void testSubmitAndFilterUsingDeepSearch() throws Exception {
        submit(trace(), trace(trace(kv("SQL", "select 1"))));
        TraceInfoSearchResult result = traceDataService.searchTraces(
                tiq("test", TraceInfoSearchQueryProxy.DEEP_SEARCH, null, 0, "SQL"));
        assertEquals(1, result.getResults().size());
    }


    // TODO test: for reverse search (if all records appear properly)

    // TODO test: sumbit non-trivial records and then do deep search

    // TODO test: submit more records and test if paging is correct

    // TODO test: search in non-existent host - should fail in a controlled way

    // TODO test: search in host without access - should fail in a controlled way

    // TODO test: search in disabled host - should fail in a controlled way
}
