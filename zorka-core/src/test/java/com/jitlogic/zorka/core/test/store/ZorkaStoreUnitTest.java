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
import org.junit.Test;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ConcurrentNavigableMap;

import static org.fest.assertions.Assertions.assertThat;

public class ZorkaStoreUnitTest extends ZorkaFixture {

    private Random r = new Random();
    private ZorkaStore store1, store2;

    public TraceRecord mktr(TraceRecord parent, String suffix, int level, int limit) {
        TraceRecord tr = new TraceRecord(null);

        tr.setClassId(symbols.symbolId("com/test/Test" + suffix + level));
        tr.setMethodId(symbols.symbolId("method" + suffix + level));
        tr.setCalls(r.nextInt(10000) + 1);
        tr.setErrors(r.nextInt(100) + 1);

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
        tr.setMarker(tm);

        return tr;
    }


    @Test
    public void testStoreReopenAndRetreiveTrace() throws Exception {
        String fname = zorka.path(getTmpDir(), "store");
        store1 = new ZorkaStore(fname, 1024, 1024, symbols);
        TraceRecord tr = mktrace("TEST", 100, 1);

        store1.open();
        store1.add(tr);
        store1.flush();

        assertThat(new File(fname).exists());

        store1.close();
        store1 = null;

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
    }


    // TODO test for removal of old items

}
