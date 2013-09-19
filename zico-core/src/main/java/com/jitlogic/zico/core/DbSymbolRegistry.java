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

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DbSymbolRegistry extends SymbolRegistry {

    private final static Logger log = LoggerFactory.getLogger(DbSymbolRegistry.class);

    private JdbcTemplate jdbc;

    @Inject
    public DbSymbolRegistry(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        loadSymbols();
    }


    private void loadSymbols() {
        jdbc.query("select * from SYMBOLS", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int sid = rs.getInt("SID");
                String name = rs.getString("NAME");
                symbolIds.put(name, sid);
                symbolNames.put(sid, name);
                if (sid > lastSymbolId.get()) {
                    lastSymbolId.set(sid);
                }
            }
        });
        log.debug("Symbols read: " + symbolIds.size());
    }


    @Override
    protected void persist(int symbolId, String symbolName) {
        jdbc.update("insert into SYMBOLS (SID,NAME) values (?,?)", symbolId, symbolName);
    }
}
