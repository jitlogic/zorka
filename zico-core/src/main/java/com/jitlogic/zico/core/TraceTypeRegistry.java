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
package com.jitlogic.zico.core;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


import com.google.inject.Singleton;
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@Singleton
public class TraceTypeRegistry {

    private SymbolRegistry symbols;

    private JdbcTemplate jdbc;

    private Map<Integer, Set<Integer>> tids;


    @Inject
    public TraceTypeRegistry(SymbolRegistry symbols, DataSource ds) {
        this.symbols = symbols;
        this.jdbc = new JdbcTemplate(ds);

        this.tids = new HashMap<Integer, Set<Integer>>();
        load();
    }


    private synchronized void load() {
        jdbc.query("select * from TRACE_TYPES", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int traceId = rs.getInt("TRACE_ID");
                if (!tids.containsKey(traceId)) {
                    tids.put(traceId, new HashSet<Integer>());
                }
                tids.get(traceId).add(rs.getInt("HOST_ID"));
            }
        });
    }


    public synchronized void mark(int traceId, int hostId) {
        if (!tids.containsKey(traceId)) {
            tids.put(traceId, new HashSet<Integer>());
        }

        if (!tids.get(traceId).contains(hostId)) {
            jdbc.update("insert into TRACE_TYPES (TRACE_ID, HOST_ID) values (?,?)", traceId, hostId);
            tids.get(traceId).add(hostId);
        }
    }


    public synchronized List<Symbol> getTidMap(Integer host) {
        List<Symbol> kv = new ArrayList<Symbol>(tids.size());

        for (Map.Entry<Integer, Set<Integer>> e : tids.entrySet()) {
            if (host == null || e.getValue().contains(host)) {
                kv.add(new Symbol(e.getKey(), symbols.symbolName(e.getKey())));
            }
        }

        return kv;
    }

}
