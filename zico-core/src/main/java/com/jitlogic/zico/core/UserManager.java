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


import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zico.core.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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


    public synchronized void open() {

        if (db != null) {
            return;
        }

        db = DBMaker.newFileDB(new File(config.getConfDir(), "users.db")).closeOnJvmShutdown().make();
        users = db.getTreeMap("USERS");

        File jsonFile = new File(config.getConfDir(), "users.json");

        if (users.size() == 0 && jsonFile.exists()) {
            log.info("User DB is empty but JSON dump file found. Importing...");
            Reader reader = null;
            try {
                reader = new FileReader(jsonFile);
                JSONObject json = new JSONObject(new JSONTokener(reader));
                JSONArray names = json.names();
                for (int i = 0; i < names.length(); i++) {
                    User user = new User(json.getJSONObject(names.getString(i)));
                    users.put(user.getUserName(), user);
                }
                db.commit();
                rebuildUserProperties();
                log.info("User DB import finished successfully.");
            } catch (IOException e) {
                log.error("Cannot import user db from JSON data", e);
            } catch (JSONException e) {
                log.error("Cannot import user db from JSON data", e);
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException e) { }
                }
            }
        }
    }


    public synchronized void close() {
        if (db != null) {
            db.close();
            db = null;
            users = null;
        }
    }


    public void export() {
        Writer writer = null;
        try {
            writer = new FileWriter(new File(config.getConfDir(), "users.json"));
            JSONObject obj = new JSONObject();
            for (Map.Entry<String,User> e : users.entrySet()) {
                obj.put(e.getKey().toString(), e.getValue().toJSONObject());
            }
            obj.write(writer);
        } catch (JSONException e) {
            log.error("Cannot export user DB", e);
        } catch (IOException e) {

        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException e) { }
            }
        }
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
        List<User> lst = new ArrayList<User>(users.size());
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
        PrintWriter out = null;
        try {
            out = new PrintWriter(f);
            for (User u : users.values()) {
                out.println(u.getUserName() + ": " + u.getPassword() + ",VIEWER"
                    + (u.hasFlag(User.ADMIN_USER) ? ",ADMIN" : ""));
            }
        } catch (IOException e) {
            log.error("Cannot write users.properties file", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
