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


import com.jitlogic.zorka.central.test.support.CentralFixture;

import com.jitlogic.zorka.common.test.support.TestTraceGenerator;
import com.jitlogic.zorka.common.tracedata.FressianTraceWriter;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoTraceOutput;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.fest.reflect.core.Reflection.*;

import static org.junit.Assert.*;

public class DataCollectionIntegTest extends CentralFixture {

    private TestTraceGenerator generator;
    private ZicoTraceOutput output;


    @Before
    public void setUpOutputAndCollector() throws Exception {
        zicoService.start();

        generator = new TestTraceGenerator();
        output = new ZicoTraceOutput(
                new FressianTraceWriter(generator.getSymbols(), generator.getMetrics()),
                "127.0.0.1", 8640, "test", "aaa");
    }


    private void submit(TraceRecord rec) {
        method("open").in(output).invoke();
        output.submit(rec);
        method("runCycle").in(output).invoke();
    }


    private int countTraces(String hostName) {
        JdbcTemplate jdbc = new JdbcTemplate(instance.getDs());

        int hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = ?", Integer.class, hostName);
        return jdbc.queryForObject("select count(1) as C from TRACES where HOST_ID = ?", Integer.class, hostId);
    }


    @Test//(timeout = 1000)
    public void testCollectSingleTraceRecord() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(instance.getDs());
        TraceRecord rec = generator.generate();

        submit(rec);

        assertEquals("One trace should be noticed.", 1, countTraces("test"));

        String tn1 = generator.getSymbols().symbolName(rec.getMarker().getTraceId());
        int remoteTID = jdbc.queryForObject("select TRACE_ID from TRACES", Integer.class);
        String tn2 = instance.getSymbolRegistry().symbolName(remoteTID);

        assertEquals(tn1, tn2);
    }


    @Test(timeout = 1000)
    public void testCollectTwoTraceRecords() throws Exception {
        submit(generator.generate());
        assertEquals("One trace should be noticed.", 1, countTraces("test"));
        submit(generator.generate());
        assertEquals("Two traces should be noticed.", 2, countTraces("test"));
    }


    @Test(timeout = 1000)
    public void testCollectBrokenTraceCausingNPE() throws Exception {
        TraceRecord rec = generator.generate();
        //rec.setMarker(null);
        rec.setFlags(0);

        submit(rec);

        assertEquals("Trace will not reach store.", 0, countTraces("test"));

        rec = generator.generate();
        submit(rec);
        assertEquals("TraceOutput should reconnect and send properly.", 1, countTraces("test"));
    }
}
