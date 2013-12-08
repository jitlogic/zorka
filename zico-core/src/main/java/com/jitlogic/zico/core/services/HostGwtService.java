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
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.util.List;


@Singleton
public class HostGwtService {

    private HostStoreManager hsm;
    private JdbcTemplate jdbc;

    @Inject
    public HostGwtService(DataSource ds, HostStoreManager hsm) {
        this.hsm = hsm;
        this.jdbc = new JdbcTemplate(ds);
    }


    public List<HostStore> findAll() {
        return hsm.list();
    }


    public List<HostStore> getUserHosts(User user) {
        return hsm.listForUser(user);
    }


    public void addUserHost(User user, HostStore host) {
        jdbc.update("insert ignore USERS_HOSTS (USER_ID, HOST_ID) values (?,?)", user.getId(), host.getId());
    }


    public void delUserHost(User user, HostStore host) {
        jdbc.update("delete from USERS_HOSTS where USER_ID = ? and HOST_ID = ?", user.getId(), host.getId());
    }

}
