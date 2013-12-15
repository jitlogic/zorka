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
package com.jitlogic.zico.core.model;


import com.jitlogic.zico.core.locators.UserLocator;

public class User  {

    public final static int ADMIN_USER = 0x0001;

    private Integer id;

    private String userName;

    private String realName;

    private String password;

    private int flags;

    private UserLocator locator;

    public User(UserLocator locator) {
        this.locator = locator;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isAdmin() {
        return 0 != (flags & ADMIN_USER);
    }

    public void setAdmin(boolean admin) {
        if (admin) {
            flags |= ADMIN_USER;
        } else {
            flags &= ~ADMIN_USER;
        }
    }

    public void persist() {
        locator.persist(this);
    }


    public void remove() {
        locator.remove(this);
    }
}
