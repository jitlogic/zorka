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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

    public final static int ADMIN_USER = 0x0001;

    private String userName;

    private String realName;

    private String password;

    private int flags;

    private List<String> allowedHosts = new ArrayList<>();


    public User() {

    }


    public User(JSONObject obj) throws JSONException {
        userName = obj.getString("username");
        realName = obj.getString("realname");
        flags = obj.getInt("flags");
        password = obj.getString("password");

        JSONArray hosts = obj.getJSONArray("hosts");

        for (int i = 0; i < hosts.length(); i++) {
            allowedHosts.add(hosts.getString(i));
        }
    }


    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("username", userName);
        json.put("realname", realName);
        json.put("password", password);
        json.put("flags", flags);

        JSONArray hosts = new JSONArray();

        for (String host : allowedHosts) {
            hosts.put(host);
        }

        json.put("hosts", hosts);

        return json;
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

    public boolean hasFlag(int flag) {
        return 0 != (flags & flag);
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


    public List<String> getAllowedHosts() {
        return allowedHosts;
    }


    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }
}
