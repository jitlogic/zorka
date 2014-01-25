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
package com.jitlogic.zico.main;


import com.jitlogic.zico.core.UserCache;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.security.Credential;
import org.mortbay.jetty.security.SSORealm;
import org.mortbay.jetty.security.UserRealm;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZicoUserRealm extends UserCache implements UserRealm, SSORealm {

    private String name;
    private Map<String,ZicoUser> users;


    public ZicoUserRealm() {
        users = new ConcurrentHashMap<String, ZicoUser>();
        UserCache.instance = this;
    }


    @Override
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    @Override
    public Principal getPrincipal(String username) {
        return users.get(username);
    }


    @Override
    public Principal authenticate(String username, Object pass, Request request) {
        ZicoUser user = users.get(username);

        if (user != null) {
            if (user.authenticate(pass)) {
                user.setAuthenticated(true);
                return user;
            }
        }

        return null;
    }


    @Override
    public boolean reauthenticate(Principal principal) {
        return ((ZicoUser)principal).isAuthenticated();
    }


    @Override
    public boolean isUserInRole(Principal principal, String role) {
        return "VIEWER".equals(role) ||
                ("ADMIN".equals(role) && principal instanceof ZicoUser && ((ZicoUser)principal).isAdmin());
    }


    @Override
    public void disassociate(Principal principal) {
    }


    @Override
    public Principal pushRole(Principal principal, String s) {
        return principal;
    }


    @Override
    public Principal popRole(Principal principal) {
        return principal;
    }


    @Override
    public void logout(Principal principal) {
        ((ZicoUser)principal).setAuthenticated(false);
    }


    @Override
    public Credential getSingleSignOn(Request request, Response response) {
        return null;
    }


    @Override
    public void setSingleSignOn(Request request, Response response, Principal principal, Credential credential) {
    }


    @Override
    public void clearSingleSignOn(String s) {
    }


    @Override
    public void update(String username, String password, boolean isAdmin) {
        users.put(username, new ZicoUser(username, password, isAdmin));
    }


    @Override
    public void remove(String username) {
        users.remove(username);
    }
}
