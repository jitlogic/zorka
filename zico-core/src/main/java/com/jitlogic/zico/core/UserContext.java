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

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class UserContext {

    // TODO this is overriding standard mechanisms (along with ZicoCacheControlFilter thread local);
    // TODO get rid of it as soon as standard guice request context stuff will work with resteasy

    public String getUser() {
        return ZicoCacheControlFilter.getRequest().getRemoteUser();
    }

    public boolean isInRole(String role) {
        return ZicoCacheControlFilter.getRequest() != null ?  ZicoCacheControlFilter.getRequest().isUserInRole(role) : true;
    }

    public void checkAdmin() {
        if (!isInRole("ADMIN")) {
            throw new ZicoRuntimeException("Insufficient privileges.");
        }
    }

}
