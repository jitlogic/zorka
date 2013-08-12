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
package com.jitlogic.zorka.central.db;


import com.jitlogic.zorka.central.CentralConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class DbContext implements Closeable {

    private CentralConfig config;
    private BasicDataSource dataSource;

    private Map<String,DbTableDesc> tables = new HashMap<String, DbTableDesc>();

    private DbTableDesc anonymousDesc = new DbTableDesc(this);

    private String dbType;

    private Pattern reSchema = Pattern.compile("@@SCHEMA@@");

    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedTemplate;


    public DbContext(CentralConfig config) {
        this.config = config;
        this.dbType = config.stringCfg("central.db.type", "h2");
        initDbContext();
    }


    private void initDbContext() {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(config.stringCfg("central.db.driver", null));
        dataSource.setUrl(config.stringCfg("central.db.url", null));
        dataSource.setUsername(config.stringCfg("central.db.user", null));
        dataSource.setPassword(config.stringCfg("central.db.pass", null));

        jdbcTemplate = new JdbcTemplate(dataSource);

        if (config.boolCfg("central.db.create", false)) {
            jdbcTemplate.execute("RUNSCRIPT FROM 'classpath:/com/jitlogic/zorka/central/" + dbType + ".createdb.sql'");
        }

        namedTemplate = new NamedParameterJdbcTemplate(dataSource);
    }


    public DbTableDesc anonymousDesc() {
        return anonymousDesc;
    }


    public DbTableDesc getNamedDesc(String name) {
        // TODO fix this when
        return anonymousDesc;
    }


    public synchronized DataSource getDataSource() {
        return dataSource;
    }


    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(getDataSource());
    }



    @Override
    public void close() throws IOException {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                // TODO log()
            }
        }
    }
}
