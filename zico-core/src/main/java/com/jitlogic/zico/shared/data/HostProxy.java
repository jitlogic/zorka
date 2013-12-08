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
import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostLocator;


@ProxyFor(value = HostStore.class, locator = HostLocator.class)
public interface HostProxy extends EntityProxy {

    Integer getId();

    String getName();

    String getAddr();

    void setAddr(String addr);

    String getDescription();

    void setDescription(String desc);

    String getPass();

    void setPass(String pass);

    int getFlags();

    void setFlags(int flags);

    boolean isEnabled();

    public void setEnabled(boolean enabled);

    long getMaxSize();

    void setMaxSize(long maxSize);
}
