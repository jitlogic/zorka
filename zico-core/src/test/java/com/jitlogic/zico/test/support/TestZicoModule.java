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
package com.jitlogic.zico.test.support;


import com.google.inject.Binder;
import com.google.inject.Provides;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.inject.AbstractZicoModule;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Singleton;

public class TestZicoModule extends AbstractZicoModule {

    private ZicoConfig config;

    public TestZicoModule(ZicoConfig config) {
        this.config = config;
    }

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
        binder.bind(UserContext.class).to(UserTestContext.class);
    }

    @Provides
    @com.google.inject.Singleton
    public BasicDataSource provideDataSource(ZicoConfig config) {
        BasicDataSource ds = super.provideDataSource(config);

        if (config.boolCfg("zico.db.create", false)) {
            new JdbcTemplate(ds).execute("RUNSCRIPT FROM 'classpath:/com/jitlogic/zico/"
                    + config.stringCfg("zico.db.type", "h2") + ".createdb.sql'");
        }

        return ds;
    }

        @Provides
    @Singleton
    public ZicoConfig provideConfig() {
        return config;
    }

}
