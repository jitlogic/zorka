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
package com.jitlogic.zico.shared.services;


import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import com.jitlogic.zico.core.ZicoServiceLocator;
import com.jitlogic.zico.core.services.SystemGwtService;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;

import java.util.List;

@Service(value = SystemGwtService.class, locator = ZicoServiceLocator.class)
public interface SystemServiceProxy extends RequestContext {

    Request<List<String>> systemInfo();

    Request<List<TraceTemplateProxy>> listTemplates();

    Request<Integer> saveTemplate(TraceTemplateProxy template);

    Request<Void> removeTemplate(Integer tid);

}
