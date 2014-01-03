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

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.UserHttpContext;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;

import javax.inject.Named;
import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class ProdZicoModule extends AbstractZicoModule {

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
        binder.bind(UserManager.class).asEagerSingleton();
        binder.bind(UserContext.class).to(UserHttpContext.class);
        binder.bind(ZicoDataProcessorFactory.class).to(HostStoreManager.class);
    }


    @Provides
    @Singleton
    public ZicoConfig provideConfig() {
        String homeDir = System.getProperty("zico.home.dir");

        if (homeDir == null) {
            throw new ZicoRuntimeException("Missing home dir configuration property. " +
                    "Add '-Dzico.home.dir=/path/to/zico/home' to JVM options.");
        }

        if (!new File(homeDir).isDirectory()) {
            throw new ZicoRuntimeException("Home dir property does not point to a directory.");
        }

        return new ZicoConfig(homeDir);
    }
}