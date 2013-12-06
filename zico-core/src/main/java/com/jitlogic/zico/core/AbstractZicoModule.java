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


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jitlogic.zico.core.services.*;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;

public abstract class AbstractZicoModule implements Module {


    @Override
    public void configure(Binder binder) {
        binder.bind(SymbolRegistry.class).to(DbSymbolRegistry.class).in(Singleton.class);
        binder.bind(DataSource.class).to(BasicDataSource.class).in(Singleton.class);

        binder.bind(AdminService.class);
        binder.bind(SystemService.class);
        binder.bind(TraceDataService.class);
        binder.bind(UserService.class);
        binder.bind(UserManager.class);
        binder.bind(UserContext.class);

        binder.bind(UserLocator.class);
        binder.bind(UserGwtService.class);
    }

    protected BasicDataSource provideDataSource(ZicoConfig config) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(config.stringCfg("zico.db.driver", null));
        ds.setUrl(config.stringCfg("zico.db.url", null));
        ds.setUsername(config.stringCfg("zico.db.user", null));
        ds.setPassword(config.stringCfg("zico.db.pass", null));
        ds.setInitialSize(0);
        ds.setMaxActive(128);
        ds.setMaxIdle(32);

        ds.setRemoveAbandonedTimeout(180 * 1000);
        ds.setRemoveAbandoned(true);
        ds.setLogAbandoned(true);

        ds.setTimeBetweenEvictionRunsMillis(30 * 1000);
        ds.setMinEvictableIdleTimeMillis(180 * 1000);
        ds.setValidationQuery("select 1");
        ds.setTestWhileIdle(true);

        return ds;
    }


    @Provides
    @Singleton
    public ZicoService provideZicoService(ZicoConfig config, HostStoreManager storeManager) {
        return new ZicoService(
                config.stringCfg("zico.listen.addr", "0.0.0.0"),
                config.intCfg("zico.listen.port", ZicoService.COLLECTOR_PORT),
                storeManager);
    }
}
