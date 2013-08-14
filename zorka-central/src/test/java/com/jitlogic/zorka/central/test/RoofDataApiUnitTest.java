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


import com.jitlogic.zorka.central.db.DbContext;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;


import java.util.*;

import static org.junit.Assert.*;

public class RoofDataApiUnitTest extends CentralFixture {


    private int hostId;

    @Before
    public void populateData() {
        JdbcTemplate jdbc = instance.getDb().getJdbcTemplate();

        jdbc.update("insert into HOSTS (HOST_NAME,HOST_ADDR,HOST_PATH) values(?,?,?)", "test", "127.0.0.1", "test");
        hostId = jdbc.queryForObject("select HOST_ID from HOSTS where HOST_NAME = 'test'", Integer.class);
        jdbc.update("insert into TRACES values (?,?,?,?,?,?,?,?,?,?,?,?)", hostId,10,1,100,1234,1,2,100,10,50,1000,"fval1|fval2|etc");
        jdbc.update("insert into TRACES values (?,?,?,?,?,?,?,?,?,?,?,?)", hostId,20,1,100,1234,1,2,100,10,50,1000,"fval1|fval2|xxx");
    }


    @Test
    public void testIfTablesHaveBeenCreated() throws Exception {
        DbContext ctx = instance.getDb();

        JdbcTemplate t = ctx.getJdbcTemplate();

        assertEquals((Object) 1, t.queryForObject("select count(1) as cnt from SYMBOLS", Integer.class));
    }


    @Test
    public void testHostJediService() {
        assertEquals(1, ((List) roofService.GET(Arrays.asList("hosts"), Collections.EMPTY_MAP)).size());
    }

    @Test
    public void testAccessTraceTableViaHost() {
        List<String> path = Arrays.asList("hosts", "" + hostId, "collections", "traces");
        List traces = (List)roofService.GET(path, Collections.EMPTY_MAP);
        assertEquals(2, traces.size());
    }

    @Test
    public void testAccessTraceTableCount() {
        List<String> path = Arrays.asList("hosts", "" + hostId, "collections", "traces", "actions", "count");
        Object count = roofService.GET(path, Collections.EMPTY_MAP);
        assertEquals(2, count);
    }

    @Test
    public void testAccessTraceTableWithOffsetAndLimit() {
        List<String> path = Arrays.asList("hosts", "" + hostId, "collections", "traces");
        Map<String,String> params = ZorkaUtil.map("limit", "1", "offset", "1");
        List traces = (List)roofService.GET(path, params);
        assertEquals(1, traces.size());
    }

    @Test
    public void testAccessHostTableCount() {
        List<String> path = Arrays.asList("hosts", "actions", "count");
        Object count = roofService.GET(path, Collections.EMPTY_MAP);
        assertEquals(1, count);
    }
}
