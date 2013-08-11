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

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DbUtil implements Closeable {

    private CentralConfig config;
    private BasicDataSource dataSource;

    private Properties dbProps;


    public DbUtil(CentralConfig config) {
        this.config = config;
    }



    public synchronized DataSource getDataSource() {
        if (dataSource == null) {

            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(config.stringCfg("central.db.driver", null));
            dataSource.setUrl(config.stringCfg("central.db.url", null));;
            dataSource.setUsername(config.stringCfg("central.db.user", null));
            dataSource.setUsername(config.stringCfg("central.db.pass", null));

            try {
                checkSchema();
            } catch (SQLException e) {
                e.printStackTrace();  // TODO log (..)
            }
        }
        return dataSource;
    }


    public synchronized Properties getDbProps() {
        if (dbProps == null) {
            dbProps = new Properties();

            InputStream is = this.getClass()
                .getResourceAsStream("/com/jitlogic/zorka/central/"
                        + config.stringCfg("central.db.type", null) + ".db.properties");

            if (is == null) {
                // TODO log ( .. )
                return null;
            }

            try {
                dbProps.load(is);
            } catch (IOException e) {
                e.printStackTrace();  // TODO log (..)
            } finally {
                try { is.close(); } catch (IOException e) { }
            }
        }

        return dbProps;
    }


    private void checkSchema() throws SQLException {
        String[] tables = getDbProps().getProperty("db.tables").split(",");
        for (String table : tables) {
            table = table.trim();
            if (select("select count(1) as c from " + table, true).size() == 0) {
                // TODO log(...)
                for (int i = 1; ; i++) {
                    String sql = dbProps.getProperty(table + ".create." + i);
                    if (sql != null) {
                        execute(sql);
                    } else {
                        break;
                    }
                }
            }
        }
    }


    public List<DbRecord> select(String query) throws SQLException {
        return select(query, false);
    }


    public List<DbRecord> select(String query, boolean quiet) throws SQLException {
        Connection conn = null;
        List<DbRecord> rslt = new ArrayList<DbRecord>();
        try {
            conn = getDataSource().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            List<String> colLabels = new ArrayList<String>();
            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                colLabels.add(rs.getMetaData().getColumnLabel(i + 1));
            }
            while (rs.next()) {
                DbRecord rec = new DbRecord();
                for (String col : colLabels) {
                    rec.put(col, rs.getObject(col));
                }
                rslt.add(rec);
            }
        } catch (SQLException e) {
            try { if (conn != null) conn.close(); conn = null; } catch (SQLException e1) { /* TODO log (...) */ }
            if (!quiet) {
                e.printStackTrace();  // TODO log (..)
                throw e;
            }
        } finally {
            try { if (conn != null) conn.close(); conn = null; } catch (SQLException e1) { /* TODO log (...) */ }
        }
        return rslt;
    }


    public void execute(String query) throws SQLException {
        execute(query, false);
    }

    public void execute(String query, boolean quiet) throws SQLException {
        Connection conn = null;
        try {
            conn = getDataSource().getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(query);
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();  // TODO log (..)
            try { if (conn != null) conn.close(); conn = null; } catch (SQLException e1) { /* TODO log (...) */ }
            throw e;
        } finally {
            try { if (conn != null) conn.close(); conn = null; } catch (SQLException e1) { /* TODO log (...) */ }
        }
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
