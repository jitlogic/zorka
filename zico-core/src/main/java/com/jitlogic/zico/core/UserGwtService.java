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


import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.List;

@Singleton
public class UserGwtService {

    private JdbcTemplate jdbc;
    private UserLocator locator;
    private UserContext userContext;


    @Inject
    public UserGwtService(DataSource ds, UserLocator locator, UserContext userContext) {
        jdbc = new JdbcTemplate(ds);
        this.locator = locator;
        this.userContext = userContext;
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


    public void resetPassword(String userName, String oldPassword, String newPassword) {
        boolean adminMode = userContext.isInRole("ADMIN");

        if (userName != null && !adminMode) {
            throw new ZicoRuntimeException("Insufficient privileges to reset other users password");
        }

        String user = (userName != null && userName.length() > 0) ? userName : userContext.getUser();

        if (!adminMode) {
            String chkHash = "MD5:" + ZorkaUtil.md5(oldPassword);

            String oldHash = jdbc.queryForObject("select PASSWORD from USERS where USER_NAME = ?", String.class, user);

            if (!chkHash.equals(oldHash)) {
                throw new ZicoRuntimeException("Invalid (old) password.");
            }
        }

        jdbc.update("update USERS set PASSWORD = ? where USER_NAME = ?", "MD5:" + ZorkaUtil.md5(newPassword), user);

    }
}
