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
package com.jitlogic.zorka.core;


import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.io.File;
import java.util.*;

public class AgentConfig extends ZorkaConfig {

    private static final ZorkaLog log = ZorkaLogger.getLog(AgentConfig.class);

    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/core/zorka.properties";
    public static final String PROP_SCRIPTS_DIR  = "zorka.scripts.dir";;
    public static final String PROP_PROFILE_DIR = "zorka.profile.dir";

    private List<String> profiles = new ArrayList<String>();
    private List<String> profileScripts = new ArrayList<String>();


    public AgentConfig(String home) {
        loadProperties(home, "zorka.properties", DEFAULT_CONF_PATH);
        setBaseProps();
        loadProfiles();
    }


    public AgentConfig(Properties props) {
        properties = props;
        homeDir = props.getProperty(PROP_HOME_DIR);
        setBaseProps();
        loadProfiles();
    }


    public List<String> getProfileScripts() {
        return Collections.unmodifiableList(profileScripts);
    }


    protected void setBaseProps() {
        if (!properties.containsKey(PROP_SCRIPTS_DIR)) {
            properties.put(PROP_SCRIPTS_DIR, ZorkaUtil.path(homeDir, "scripts"));
        }

        if (!properties.containsKey(PROP_PROFILE_DIR)) {
            properties.put(PROP_PROFILE_DIR, ZorkaUtil.path(homeDir, "profiles"));
        }

        if (!properties.containsKey("zorka.log.dir")) {
            properties.put("zorka.log.dir", ZorkaUtil.path(homeDir, "log"));
        }
    }



    /**
     * Loads selected profiles and merges their properties with main configuration.
     */
    private void loadProfiles() {
        profiles = listCfg("profiles");

        for (String profile : profiles) {
            File f = new File(ZorkaUtil.path(stringCfg(PROP_PROFILE_DIR, "/"), profile + ".profile"));
            if (f.exists() && f.canRead()) {
                log.info(ZorkaLogger.ZAG_CONFIG, "Loading profile: " + profile);
                Properties props = loadCfg(new Properties(), f.getPath(), true);
                profileScripts.addAll(listCfg(props, "profile.scripts"));
                props.remove("profile.scripts");
                for (Map.Entry<Object,Object> e : props.entrySet()) {
                    properties.setProperty(e.getKey().toString(), e.getValue().toString());
                }
            } else {
                log.error(ZorkaLogger.ZAG_CONFIG, "Cannot load profile " + profile + ": file " + f + " does not exist.");
            }
        }

    }

    @Override
    protected void markError(String msg, Throwable e) {
        super.markError(msg, e);
        AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
    }
}
