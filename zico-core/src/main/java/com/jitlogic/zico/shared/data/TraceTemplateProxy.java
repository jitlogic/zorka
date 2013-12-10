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
package com.jitlogic.zico.shared.data;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zico.core.locators.TraceTemplateManager;

@ProxyFor(value = TraceTemplate.class, locator = TraceTemplateManager.class)
public interface TraceTemplateProxy extends EntityProxy {

    public int getId();

    public void setId(int id);

    public int getTraceId();

    public void setTraceId(int id);

    public int getOrder();

    public void setOrder(int order);

    public int getFlags();

    public void setFlags(int flags);

    public String getCondTemplate();

    public void setCondTemplate(String condTemplate);

    public String getCondRegex();

    public void setCondRegex(String condRegex);

    public String getTemplate();

    public void setTemplate(String template);


}
