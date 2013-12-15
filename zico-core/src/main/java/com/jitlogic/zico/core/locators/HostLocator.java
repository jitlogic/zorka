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
package com.jitlogic.zico.core.locators;


import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.ZicoEntity;
import com.jitlogic.zico.core.ZicoRuntimeException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;

public class HostLocator extends Locator<HostStore, Integer> implements ZicoEntity<HostStore> {

    private JdbcTemplate jdbc;
    private SimpleJdbcInsert jdbci;

    private HostStoreManager hsm;

    @Inject
    public HostLocator(DataSource ds, HostStoreManager hsm) {
        jdbc = new JdbcTemplate(ds);
        jdbci = new SimpleJdbcInsert(ds).withTableName("HOSTS").usingGeneratedKeyColumns("HOST_ID")
                .usingColumns("HOST_NAME", "HOST_ADDR", "HOST_PATH", "HOST_DESC", "HOST_PASS", "HOST_FLAGS", "MAX_SIZE");
        this.hsm = hsm;
    }

    @Override
    public HostStore create(Class<? extends HostStore> clazz) {
        throw new ZicoRuntimeException("Not implemented.");
    }

    @Override
    public HostStore find(Class<? extends HostStore> clazz, Integer id) {
        return hsm.getHost(id);
    }

    @Override
    public Class<HostStore> getDomainType() {
        return HostStore.class;
    }

    @Override
    public Integer getId(HostStore host) {
        return host.getHostInfo().getId();
    }

    @Override
    public Class<Integer> getIdType() {
        return Integer.class;
    }

    @Override
    public Object getVersion(HostStore host) {
        return 1;
    }

    @Override
    public void persist(HostStore host) {
        host.save();
    }

    @Override
    public void remove(HostStore host) {
        try {
            hsm.delete(host.getHostInfo().getId());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
