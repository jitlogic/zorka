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
package com.jitlogic.zorka.central;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jitlogic.zorka.central.rest.AdminService;
import com.jitlogic.zorka.central.rest.SystemService;
import com.jitlogic.zorka.central.rest.TraceDataService;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.zico.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public abstract class AbstractCentralModule implements Module {


    @Override
    public void configure(Binder binder) {
        binder.bind(SymbolRegistry.class).to(DbSymbolRegistry.class).in(Singleton.class);
        binder.bind(DataSource.class).to(BasicDataSource.class).in(Singleton.class);

        binder.bind(AdminService.class);
        binder.bind(SystemService.class);
        binder.bind(TraceDataService.class);
    }

    @Provides
    @Singleton
    public BasicDataSource provideDataSource(CentralConfig config) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(config.stringCfg("central.db.driver", null));
        ds.setUrl(config.stringCfg("central.db.url", null));
        ds.setUsername(config.stringCfg("central.db.user", null));
        ds.setPassword(config.stringCfg("central.db.pass", null));

        if (config.boolCfg("central.db.create", false)) {
            new JdbcTemplate(ds).execute("RUNSCRIPT FROM 'classpath:/com/jitlogic/zorka/central/"
                    + config.stringCfg("central.db.type", "h2") + ".createdb.sql'");
        }

        return ds;
    }


    @Provides
    @Singleton
    public ZicoService provideZicoService(CentralConfig config, HostStoreManager storeManager) {
        return new ZicoService(
                config.stringCfg("central.listen.addr", "0.0.0.0"),
                config.intCfg("central.listen.port", ZicoService.COLLECTOR_PORT),
                storeManager);
    }
}
