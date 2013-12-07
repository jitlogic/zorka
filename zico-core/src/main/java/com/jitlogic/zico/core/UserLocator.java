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


import com.google.inject.Singleton;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class UserLocator extends Locator<User, Integer> implements RowMapper<User>, ZicoEntity<User> {

    private JdbcTemplate jdbc;
    private SimpleJdbcInsert jdbci;


    // TODO inject it via injector, not default instanatiation
    public UserLocator() {
        DataSource ds = ZicoServiceLocator.injector.getInstance(DataSource.class);
        jdbc = new JdbcTemplate(ds);
        jdbci = new SimpleJdbcInsert(ds).withTableName("USERS").usingGeneratedKeyColumns("USER_ID")
                .usingColumns("USER_NAME", "REAL_NAME", "FLAGS", "PASSWORD");
    }

//    @Inject
//    public UserLocator(DataSource ds) {
//        jdbc = new JdbcTemplate(ds);
//    }

    @Override
    public User create(Class<? extends User> aClass) {
        return new User(this);
    }

    @Override
    public User find(Class<? extends User> clazz, Integer id) {
        try {
            return jdbc.queryForObject("select * from USERS where USER_ID = ?", this, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Class<User> getDomainType() {
        return User.class;
    }


    @Override
    public Integer getId(User user) {
        return user.getId();
    }


    @Override
    public Class<Integer> getIdType() {
        return Integer.class;
    }


    @Override
    public Object getVersion(User user) {
        return 1;
    }


    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User(this);

        user.setId(rs.getInt("USER_ID"));
        user.setUserName(rs.getString("USER_NAME"));
        user.setRealName(rs.getString("REAL_NAME"));
        user.setPassword(rs.getString("PASSWORD"));
        user.setFlags(rs.getInt("FLAGS"));

        return user;
    }


    public void persist(User user) {

        if (user.getId() != null) {
            jdbc.update("update USERS set USER_NAME = ?, REAL_NAME = ?, PASSWORD = ?, FLAGS = ? where USER_ID = ?",
                    user.getUserName(), user.getRealName(), user.getPassword(), user.getFlags(), user.getId());
        } else {

            if (user.getPassword() == null) {
                user.setPassword("MD5:00000000000000000000000000000000");
            }

            Number id = jdbci.executeAndReturnKey(ZorkaUtil.<String, Object>map(
                    "USER_NAME", user.getUserName(),
                    "REAL_NAME", user.getRealName(),
                    "PASSWORD", user.getPassword(),
                    "FLAGS", user.getFlags()
            ));
            user.setId(id.intValue());

            int vid = jdbc.queryForObject("select GROUP_ID from GROUPS where GROUP_NAME = ?", Integer.class, "VIEWER");
            jdbc.update("insert into USERS_GROUPS (USER_ID,GROUP_ID) values (?,?)", user.getId(), vid);
        }

        int gid = jdbc.queryForObject("select GROUP_ID from GROUPS where GROUP_NAME = ?", Integer.class, "ADMIN");

        jdbc.update("delete from USERS_GROUPS where USER_ID = ? and GROUP_ID = ?", user.getId(), gid);

        if (user.isAdmin()) {
            jdbc.update("insert into USERS_GROUPS (USER_ID,GROUP_ID) values (?,?)", user.getId(), gid);
        }
    }


    public void remove(User user) {
        if (user.getId() != null) {
            jdbc.update("delete from USERS where USER_ID = ?", user.getId());
        }
    }
}
