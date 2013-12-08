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
package com.jitlogic.zico.client.inject;


import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.inject.Provides;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.api.AdminApi;
import com.jitlogic.zico.client.api.TraceDataApi;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestServiceProxy;

import javax.inject.Singleton;

public class ClientModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(ErrorHandler.class);
        bind(EventBus.class).to(SimpleEventBus.class);
        bind(ZicoShell.class).in(Singleton.class);
        install(new GinFactoryModuleBuilder().build(PanelFactory.class));
    }


    @Provides
    @Singleton
    AdminApi provideAdminApi() {
        AdminApi adminApi = GWT.create(AdminApi.class);
        ((RestServiceProxy) adminApi).setResource(new Resource(GWT.getHostPageBaseURL() + "rest"));
        return adminApi;
    }


    @Provides
    @Singleton
    TraceDataApi providesTraceDataApi() {
        TraceDataApi traceDataApi = GWT.create(TraceDataApi.class);
        ((RestServiceProxy) traceDataApi).setResource(new Resource(GWT.getHostPageBaseURL() + "rest"));
        return traceDataApi;
    }


    @Provides
    @Singleton
    ZicoRequestFactory provideRequestFactory(EventBus bus) {
        ZicoRequestFactory factory = GWT.create(ZicoRequestFactory.class);
        factory.initialize(bus);
        return factory;
    }
}
