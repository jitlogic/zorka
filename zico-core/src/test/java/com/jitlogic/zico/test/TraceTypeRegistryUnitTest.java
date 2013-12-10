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
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.test.support.ZicoFixture;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.fest.assertions.Assertions.assertThat;

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


    @Test
    public void testReadAndQueryRegistry() {
        TraceTypeRegistry ttr = new TraceTypeRegistry(symbolRegistry, dataSource);
        assertThat(ttr.getTidMap(1)).hasSize(2)
            .contains(new KeyValuePair("" + e, "EJB"), new KeyValuePair("" + h, "HTTP"));

        assertThat(ttr.getTidMap(null)).hasSize(3)
            .contains(new KeyValuePair(""+e, "EJB"), new KeyValuePair(""+h, "HTTP"), new KeyValuePair(""+s, "SQL"));
    }


    @Test
    public void testMarkNewTraceHostPairsAndCheckIfRememberedAndSaved() {
        TraceTypeRegistry ttr = new TraceTypeRegistry(symbolRegistry, dataSource);

        assertThat(ttr.getTidMap(1)).hasSize(2)
            .contains(new KeyValuePair(""+e, "EJB"), new KeyValuePair(""+h, "HTTP"));

        assertEquals(0, (int)jdbc.queryForObject("select count(1) from TRACE_TYPES where HOST_ID = ? and TRACE_ID = ?",
                Integer.class, 1, s));

        ttr.mark(s, 1);

        assertThat(ttr.getTidMap(1)).hasSize(3)
            .contains(new KeyValuePair(""+e, "EJB"), new KeyValuePair(""+h, "HTTP"), new KeyValuePair(""+s, "SQL"));

        assertEquals(1, (int)jdbc.queryForObject("select count(1) from TRACE_TYPES where HOST_ID = ? and TRACE_ID = ?",
                Integer.class, 1, s));

    }
}
