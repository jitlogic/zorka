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


import com.google.inject.Singleton;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zico.core.model.User;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

@Singleton
public class UserManager extends Locator<User, String> {

    private final static Logger log = LoggerFactory.getLogger(UserManager.class);

    private DB db;
    private ConcurrentNavigableMap<String,User> users;

    private ZicoConfig config;

    @Inject
    public UserManager(ZicoConfig config) {
        this.config = config;

        open();
    }

    public void open() {
        db = DBMaker.newFileDB(new File(config.getConfDir(), "users.db")).closeOnJvmShutdown().make();
        users = db.getTreeMap("USERS");
    }

    public void close() {
        db.close();
        db = null; users = null;
    }

    @Override
    public User create(Class<? extends User> aClass) {
        return new User();
    }


    @Override
    public User find(Class<? extends User> clazz, String username) {
        return users.get(username);
    }


    @Override
    public Class<User> getDomainType() {
        return User.class;
    }


    @Override
    public String getId(User user) {
        return user.getUserName();
    }


    @Override
    public Class<String> getIdType() {
        return String.class;
    }


    @Override
    public Object getVersion(User user) {
        return 1;
    }

    public List<User> findAll() {
        List<User> lst = new ArrayList<>(users.size());
        lst.addAll(users.values());
        return lst;
    }

    public void persist(User user) {
        users.put(user.getUserName(), user);
        rebuildUserProperties();
        db.commit();
    }


    public void remove(User user) {
        users.remove(user.getUserName());
        rebuildUserProperties();
        db.commit();
    }

    private void rebuildUserProperties() {
        File f = new File(config.getHomeDir(), "users.properties");
        try (PrintWriter out = new PrintWriter(f)) {
            for (User u : users.values()) {
                out.println(u.getUserName() + ": " + u.getPassword() + ",VIEWER"
                    + (u.hasFlag(User.ADMIN_USER) ? ",ADMIN" : ""));
            }
        } catch (IOException e) {
            log.error("Cannot write users.properties file", e);
        }
    }
}
