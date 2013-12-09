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

import com.jitlogic.zico.core.TraceTypeRegistry;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.*;

public class TraceTypeRegistryUnitTest extends ZicoFixture {

    private JdbcTemplate jdbc;
    private int e;
    private int h;
    private int s;

    @Before
    public void initTraceTypesTable() {
        jdbc = new JdbcTemplate(dataSource);

        e = symbolRegistry.symbolId("EJB");
        h = symbolRegistry.symbolId("HTTP");
        s = symbolRegistry.symbolId("SQL");

        for (int[] ht : new int[][]{{1, e}, {1, h}, {2, s}}) {
            jdbc.update("insert into TRACE_TYPES (HOST_ID,TRACE_ID) values (?,?)", ht[0], ht[1]);
        }
    }

    @Test @Ignore("To be fixed.")
    public void testReadAndQueryRegistry() {
        TraceTypeRegistry ttr = new TraceTypeRegistry(symbolRegistry, dataSource);
        assertEquals(ZorkaUtil.<Integer, String>map(e, "EJB", h, "HTTP"), ttr.getTidMap(1));
        assertEquals(ZorkaUtil.<Integer, String>map(e, "EJB", h, "HTTP", s, "SQL"), ttr.getTidMap(null));
    }

    @Test @Ignore("TO be fixed.")
    public void testMarkNewTraceHostPairsAndCheckIfRememberedAndSaved() {
        TraceTypeRegistry ttr = new TraceTypeRegistry(symbolRegistry, dataSource);

        assertEquals(ZorkaUtil.<Integer, String>map(e, "EJB", h, "HTTP"), ttr.getTidMap(1));
        assertEquals(0, jdbc.queryForInt("select count(1) from TRACE_TYPES where HOST_ID = ? and TRACE_ID = ?", 1, s));
        ttr.mark(s, 1);
        assertEquals(ZorkaUtil.<Integer, String>map(e, "EJB", h, "HTTP", s, "SQL"), ttr.getTidMap(1));
        assertEquals(1, jdbc.queryForInt("select count(1) from TRACE_TYPES where HOST_ID = ? and TRACE_ID = ?", 1, s));

    }
}
