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
package com.jitlogic.zico.core.services;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.User;
import com.jitlogic.zico.core.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Singleton
public class HostGwtService {

    private HostStoreManager hsm;
    private JdbcTemplate jdbc;
    private UserContext ctx;

    @Inject
    public HostGwtService(DataSource ds, HostStoreManager hsm, UserContext ctx) {
        this.hsm = hsm;
        this.jdbc = new JdbcTemplate(ds);
        this.ctx = ctx;
    }


    public List<HostStore> findAll() {
        if (ctx.isInRole("ADMIN")) {
            return hsm.list();
        } else {
            List<HostStore> lst = new ArrayList<HostStore>();
            int uid = jdbc.queryForObject("select USER_ID from USERS where USER_NAME = ?", Integer.class, ctx.getUser());
            final Set<Integer> allowedHosts = new HashSet<Integer>();

            jdbc.query("select HOST_ID from USERS_HOSTS where USER_ID = ?", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet resultSet) throws SQLException {
                    allowedHosts.add(resultSet.getInt("HOST_ID"));
                }
            }, uid);

            for (HostStore host : hsm.list()) {
                if (allowedHosts.contains(host.getId())) {
                    lst.add(host);
                }
            }

            return lst;
        }
    }


    public List<HostStore> getUserHosts(User user) {
        return hsm.listForUser(user);
    }


    public void addUserHost(User user, HostStore host) {
        ctx.checkAdmin();
        jdbc.update("insert ignore USERS_HOSTS (USER_ID, HOST_ID) values (?,?)", user.getId(), host.getId());
    }


    public void delUserHost(User user, HostStore host) {
        ctx.checkAdmin();
        jdbc.update("delete from USERS_HOSTS where USER_ID = ? and HOST_ID = ?", user.getId(), host.getId());
    }

    public void persist(HostStore host) {
        host.save();
    }

    public void remove(HostStore host) {
        try {
            hsm.delete(host.getId());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
