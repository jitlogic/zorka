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

package com.jitlogic.zorka.core.test.store;

import com.jitlogic.zorka.core.spy.TraceMarker;
import com.jitlogic.zorka.core.spy.TraceRecord;
import com.jitlogic.zorka.core.store.TraceEntry;
import com.jitlogic.zorka.core.store.ZorkaStore;
import com.jitlogic.zorka.core.test.support.ZorkaFixture;
import com.jitlogic.zorka.core.util.ZorkaUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

import static org.fest.assertions.Assertions.assertThat;

public class ZorkaStoreUnitTest extends ZorkaFixture {

    private ZorkaStore store1, store2;

    private static final long MB = 1024*1024;

    public TraceRecord mktr(TraceRecord parent, String suffix, int level, int limit) {
        TraceRecord tr = new TraceRecord(null);

        tr.setClassId(symbols.symbolId("com/test/Test" + suffix + level));
        tr.setMethodId(symbols.symbolId("method" + suffix + level));
        tr.setCalls(level*13+limit*17);
        tr.setErrors(level*3+limit);

        if (level < limit) {
            for (int i = 0; i < level; i++) {
                tr.addChild(mktr(tr, suffix, level + 1, limit));
            }
        }

        return tr;
    }


    public TraceRecord mktrace(String name, long clock, int levels) {
        TraceRecord tr = mktr(null, name, 0, levels);
        TraceMarker tm = new TraceMarker(tr, symbols.symbolId(name), clock);
        tr.markFlag(TraceRecord.TRACE_BEGIN);
        tr.setMarker(tm);

        return tr;
    }


    private void wrtrace(String fname, long maxsz, long clock, int levels, int records) throws IOException {
        store1 = new ZorkaStore(fname, maxsz, maxsz*10, symbols);

        store1.open();

        for (int i = 0; i < records; i++) {
            TraceRecord tr = mktrace("TEST", clock*(i+1), levels);
            store1.add(tr);
        }

        store1.flush();

        store1.close();
        store1 = null;
    }


    @Test
    public void testStoreReopenAndRetreiveTrace() throws Exception {
        String fname = zorka.path(getTmpDir(), "store");
        wrtrace(fname, 100*MB, 100, 1, 1);

        assertThat(new File(fname).exists());


        store2 = new ZorkaStore(fname, 1024, 1024, symbols);
        store2.open();

        assertThat(store2.firstTs()).isEqualTo(100L);
        assertThat(store2.lastTs()).isEqualTo(100L);

        ConcurrentNavigableMap<Long,TraceEntry> entries = store2.findByTs(100L, 100L);
        assertThat(entries).isNotNull().isNotEmpty();

        TraceEntry entry = entries.firstEntry().getValue();
        assertThat(entry.getTstamp()).isEqualTo(100L);
        assertThat(entry.getPos()).isEqualTo(0);
        assertThat(entry.getLen()).isGreaterThan(0);

        TraceRecord rec = store2.getTrace(entry);
        assertThat(rec).isNotNull();
        assertThat(rec.getMarker()).isNotNull();
        assertThat(rec.getMarker().getClock()).isEqualTo(100L);
    }


    private List<String> path(String...args) {
        return Arrays.asList(args);
    }


    private Map<String,String> par(String...args) {
        return ZorkaUtil.map(args);
    }


    @Test
    public void testListItemViaRestfulIface() throws Exception {
        String fname = zorka.path(getTmpDir(), "store");
        wrtrace(fname, 100*MB, 100, 1, 1);

        store2 = new ZorkaStore(fname, 1024, 1024, symbols);
        store2.open();

        assertThat(store2.get("/traces", par())).isEqualTo(
            Arrays.asList(ZorkaUtil.map(
                "id", 1L, "time", "0.00ms", "errors", 1L,
                "label", "TEST", "recs", 1L, "calls", 17L,
                "tstamp", "1970-01-01 01:00:00.100")));

        assertThat(store2.get("/traces", par("offs", "1"))).isEqualTo(
            Arrays.asList(new Map[0]));
    }

    @Test
    public void testListMoreItemsViaRestfulIface() throws Exception {
        String fname = zorka.path(getTmpDir(), "store");
        wrtrace(fname, 100*MB, 10000, 1, 100);

        store2 = new ZorkaStore(fname, 1024, 1024, symbols);
        store2.open();

        assertThat(store2.get("/traces", par("offs", "0", "limit", "2"))).isEqualTo(
                Arrays.asList(
                        ZorkaUtil.map("id", 100L, "time", "0.00ms", "errors", 1L,
                                "label", "TEST", "recs", 1L, "calls", 17L, "tstamp", "1970-01-01 01:16:40.000"),
                        ZorkaUtil.map("id", 99L, "time", "0.00ms", "errors", 1L,
                                "label", "TEST", "recs", 1L, "calls", 17L, "tstamp", "1970-01-01 01:16:30.000")
                ));

        assertThat(store2.get("/traces", par("offs", "50", "limit", "2"))).isEqualTo(
                Arrays.asList(
                        ZorkaUtil.map("id", 50L, "time", "0.00ms", "errors", 1L,
                                "label", "TEST", "recs", 1L, "calls", 17L, "tstamp", "1970-01-01 01:08:20.000"),
                        ZorkaUtil.map("id", 49L, "time", "0.00ms", "errors", 1L,
                                "label", "TEST", "recs", 1L, "calls", 17L, "tstamp", "1970-01-01 01:08:10.000")
                ));
    }
}
