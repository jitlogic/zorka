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
package com.jitlogic.zorka.central.test;


import com.jitlogic.zorka.central.ReceiverContext;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.jitlogic.zorka.central.rest.TraceDataApi;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import com.jitlogic.zorka.common.test.support.TestTraceGenerator;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.TraceRecord;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.Assert.*;

public class TraceDataApiUnitTest extends CentralFixture {

    TraceDataApi api;

    @Before
    public void prepareData() throws Exception {
        JdbcTemplate jdbc = instance.getDb().getJdbcTemplate();
        ReceiverContext rcx = new ReceiverContext(jdbc, instance.getStoreManager().get("test"));
        TestTraceGenerator generator = new TestTraceGenerator();
        TraceRecord tr = generator.generate();
        Symbol s1 = new Symbol(tr.getClassId(), generator.getSymbols().symbolName(tr.getClassId()));
        rcx.process(s1);
        rcx.process(new Symbol(tr.getMethodId(), generator.getSymbols().symbolName(tr.getMethodId())));
        rcx.process(new Symbol(tr.getSignatureId(), generator.getSymbols().symbolName(tr.getSignatureId())));
        rcx.process(tr);

        api = new TraceDataApi();
    }


    @Test
    public void testGetTraceRoot() throws Exception {
        TraceRecordInfo tr = api.getRecord(api.getOrCreateHost("test", "").getId(), 0, "");
        assertEquals(0, tr.getChildren());
    }

    @Test
    public void testListTraceRoot() throws Exception {
        List<TraceRecordInfo> lst = api.listRecords(api.getOrCreateHost("test", "").getId(), 0, "");
        assertNotNull(lst);
    }
}
