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
package com.jitlogic.zico.core.inject;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.TraceTableWriter;
import com.jitlogic.zico.core.ZicoService;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;


public class ZicoBootstrapListener extends GuiceServletContextListener {

    private final static Logger log = LoggerFactory.getLogger(ZicoBootstrapListener.class);

    public static class ZicoServletModule extends ServletModule {
        @Override
        protected void configureServlets() {
            serve("/gwtRequest").with(ZicoRequestFactoryServlet.class);
        }
    }

    @Override
    protected Injector getInjector() {
        final Injector injector = Guice.createInjector(new ProdZicoModule(), new ZicoServletModule());

        injector.getInstance(ZicoService.class).start();
        injector.getInstance(TraceTableWriter.class).start();
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

        return injector;
    }
}
