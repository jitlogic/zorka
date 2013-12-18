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
package com.jitlogic.zico.core.services;


import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.UserManager;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zico.core.model.User;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class UserGwtService {

    private UserManager locator;
    private UserContext userContext;

    @Inject
    public UserGwtService(UserManager locator, UserContext userContext) {
        this.locator = locator;
        this.userContext = userContext;
    }

    public List<User> findAll() {
        return locator.findAll();
    }

    public User findUser(String username) {
        return locator.find(User.class, username);
    }


    public List<String> getAllowedHosts(String username) {
        User user = locator.find(User.class, username);
        if (user != null && user.getAllowedHosts() != null) {
            List<String> lst = new ArrayList<>();
            lst.addAll(user.getAllowedHosts());
            return lst;
        }
        return Collections.EMPTY_LIST;
    }


    public void setAllowedHosts(String username, List<String> hosts) {
        User user = locator.find(User.class, username);
        user.setAllowedHosts(hosts);
        locator.persist(user);
    }


    public void persist(User user) {
        locator.persist(user);
    }


    public void remove(User user) {
        locator.remove(user);
    }


    public void resetPassword(String userName, String oldPassword, String newPassword) {
        boolean adminMode = userContext.isInRole("ADMIN");

        if (userName != null && !adminMode) {
            throw new ZicoRuntimeException("Insufficient privileges to reset other users password");
        }

        User user = locator.find (User.class,
                (userName != null && userName.length() > 0) ? userName : userContext.getUser());

        if (!adminMode) {
            String chkHash = "MD5:" + ZorkaUtil.md5(oldPassword);

            String oldHash = user.getPassword();

            if (!chkHash.equals(oldHash)) {
                throw new ZicoRuntimeException("Invalid (old) password.");
            }
        }

        user.setPassword("MD5:" + ZorkaUtil.md5(newPassword));
        locator.persist(user);
    }

    public boolean isAdminMode() {
        return userContext.isInRole("ADMIN");
    }
}
