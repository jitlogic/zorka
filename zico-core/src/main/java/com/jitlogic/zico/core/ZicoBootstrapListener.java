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

import com.google.inject.Injector;
import com.jitlogic.zorka.common.zico.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;


public class ZicoBootstrapListener extends GuiceResteasyBootstrapServletContextListener {

    private final static Logger log = LoggerFactory.getLogger(ZicoBootstrapListener.class);

    public void withInjector(final Injector injector) {
        injector.getInstance(ZicoService.class).start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting down Zorka Intranet Collector ...");
                injector.getInstance(ZicoService.class).stop();

                try {
                    injector.getInstance(HostStoreManager.class).close();
                } catch (IOException e) {
                    log.error("Error closing host store manager", e);
                }

                try {
                    injector.getInstance(BasicDataSource.class).close();
                } catch (SQLException e) {
                    log.error("Error closing database connection", e);
                }
            }
        });
    }
}
