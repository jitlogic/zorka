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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.List;

@Singleton
public class UserGwtService {

    private JdbcTemplate jdbc;
    private UserLocator locator;


    @Inject
    public UserGwtService(DataSource ds, UserLocator locator) {
        jdbc = new JdbcTemplate(ds);
        this.locator = locator;
    }


    public Integer count() {
        return jdbc.queryForObject("select count(1) from USERS", Integer.class);
    }


    public List<User> findAll() {
        return jdbc.query("select * from USERS", locator);
    }


    public List<Integer> getAllowedHostIds(Integer userId) {
        List<Integer> lst = jdbc.queryForList("select HOST_ID from USERS_HOSTS where USER_ID = ?", Integer.class, userId);
        return lst;
    }


    public void setAllowedHostIds(Integer userId, List<Integer> hostIds) {
        jdbc.update("delete from USERS_HOSTS where USER_ID = ?", userId);

        for (Integer hostId : hostIds) {
            jdbc.update("insert into USERS_HOSTS (USER_ID, HOST_ID) values (?,?)", userId, hostId);
        }
    }


    public void persist(User user) {
        locator.persist(user);
    }


    public void remove(User user) {
        locator.remove(user);
    }
}
