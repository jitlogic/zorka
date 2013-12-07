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


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class UserManager {

    private JdbcTemplate jdbc;

    @Inject
    public UserManager(DataSource ds) {
        jdbc = new JdbcTemplate(ds);
    }

    public Set<Integer> getAllowedHosts(String username) {
        final Set<Integer> ret = new HashSet<Integer>();

        Integer userId = jdbc.queryForObject("select USER_ID from USERS where USER_NAME = ?", Integer.class, username);

        jdbc.query("select HOST_ID from USERS_HOSTS where USER_ID = ?", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                ret.add(rs.getInt("HOST_ID"));
            }
        }, userId);

        return ret;
    }

}
