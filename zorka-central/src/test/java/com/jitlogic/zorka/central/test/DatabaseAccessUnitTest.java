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
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class DatabaseAccessUnitTest extends CentralFixture {

    @Test
    public void testIfTablesHaveBeenCreated() throws Exception {
        DbContext ctx = instance.getDb();

        JdbcTemplate t = ctx.getJdbcTemplate();

        assertEquals((Object) 1, t.queryForObject("select count(1) as cnt from SYMBOLS", Integer.class));
    }

    @Test
    public void testHostJediService() {
        assertEquals(0, ((List) roofService.GET(Arrays.asList("hosts"), new HashMap<String, String>())).size());
        hostTable.getHost("test", "test");
        assertEquals(1, ((List) roofService.GET(Arrays.asList("hosts"), new HashMap<String, String>())).size());
    }

    @Test
    public void testAccessTraceTableViaHost() {

    }

}
