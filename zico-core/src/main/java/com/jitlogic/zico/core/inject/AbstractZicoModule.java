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
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;


public abstract class AbstractZicoModule implements Module {


    @Override
    public void configure(Binder binder) {
        binder.bind(ZicoRequestFactoryServlet.class);
        binder.bind(ZicoDataProcessorFactory.class).to(HostStoreManager.class);
    }


    @Provides
    @Singleton
    public ZicoService provideZicoService(ZicoDataProcessorFactory zcf, ZicoConfig config) {
        return new ZicoService(zcf,
                config.stringCfg("zico.listen.addr", "0.0.0.0"),
                config.intCfg("zico.listen.port", ZicoService.COLLECTOR_PORT),
                config.intCfg("zico.threads.max", 32),
                config.intCfg("zico.socket.timeout", 30000));
    }
}
