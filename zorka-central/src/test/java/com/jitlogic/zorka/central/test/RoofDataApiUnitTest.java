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


import com.jitlogic.zorka.central.rest.TraceDataApi;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;


import static org.junit.Assert.*;

public class RoofDataApiUnitTest extends CentralFixture {


    private int hostId;
    private TraceDataApi api;
    private JdbcTemplate jdbc;


    @Before
    public void populateData() {
        jdbc = new JdbcTemplate(instance.getDs());

        jdbc.update("insert into HOSTS (HOST_NAME,HOST_ADDR,HOST_PATH) values(?,?,?)", "test", "127.0.0.1", "test");
        hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = 'test'", Integer.class);
        jdbc.update("insert into TRACES values (?,?,?,?,?,?,?,?,?,?,?,?)", hostId, 10, 1, 100, 1234, 1, 2, 100, 10, 50, 1000, "fval1|fval2|etc");
        jdbc.update("insert into TRACES values (?,?,?,?,?,?,?,?,?,?,?,?)", hostId, 20, 1, 100, 1234, 1, 2, 100, 10, 50, 1000, "fval1|fval2|xxx");
        api = new TraceDataApi();
    }


    @Test
    public void testIfTablesHaveBeenCreated() throws Exception {
        assertEquals((Object) 1, jdbc.queryForObject("select count(1) as cnt from SYMBOLS", Integer.class));
    }


    @Test
    public void testListHostsViaApi() {
        assertEquals(1, api.getHosts().size());
    }


    @Test
    public void testAccessTraceTableViaHost() {
        assertEquals(2, api.listTraces(hostId, 0, 100).size());
    }


    @Test
    public void testAccessTraceTableCount() {
        assertEquals(2, api.countTraces(hostId));
    }

    @Test
    public void testAccessTraceTableWithOffsetAndLimit() {
        assertEquals(1, api.listTraces(hostId, 1, 1).size());
    }

}
