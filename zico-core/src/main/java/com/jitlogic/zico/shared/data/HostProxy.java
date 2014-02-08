/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.jitlogic.zico.core.HostStoreManager;


@ProxyFor(value = HostStore.class, locator = HostStoreManager.class)
public interface HostProxy extends EntityProxy {

    /**
     * This flag indicates that host is offline. Performance data cannot be read nor written, host info cannot be
     * modified (except for switching host back online). As all data files are closed, maintenance tasks can be
     * executed (eg. backup, database repair etc.).
     */
    public static final int DISABLED = 0x0001;

    /**
     * Datastore check in progress.
     */
    public static final int CHK_IN_PROGRESS = 0x0004;

    public static final int DELETED = 0x0008;

    String getName();

    String getAddr();

    void setAddr(String addr);

    String getPass();

    void setPass(String pass);

    int getFlags();

    boolean isEnabled();

    public void setEnabled(boolean enabled);

    long getMaxSize();

    void setMaxSize(long maxSize);

    public String getComment();

    public void setComment(String comment);
}
