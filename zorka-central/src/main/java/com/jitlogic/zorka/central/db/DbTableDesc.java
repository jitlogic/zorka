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


import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DbTableDesc implements RowMapper<DbRecord> {

    private DbContext db;
    private String tableName;

    private List<String> pk = new ArrayList<String>();


    public DbTableDesc(DbContext db) {
        this.db = db;
        this.tableName = "";
        this.pk = Collections.unmodifiableList(new ArrayList<String>());
    }


    public DbTableDesc(DbContext db, String tableName, String...pk) {
        this.db = db;
        this.tableName = tableName;
        this.pk = Collections.unmodifiableList(Arrays.asList(pk));
    }


    @Override
    public DbRecord mapRow(ResultSet rs, int n) throws SQLException {
        DbRecord record = new DbRecord(this);

        for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
            record.put(rs.getMetaData().getColumnName(i+1), rs.getObject(i+1));
        }

        return record;
    }


    public String getTableName() {
        return tableName;
    }


    public List<String> getPk() {
        return pk;
    }

}
