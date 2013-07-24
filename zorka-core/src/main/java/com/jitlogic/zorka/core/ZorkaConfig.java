/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.core.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.util.ZorkaLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zorka Configuration handling class. 
 * 
 * All configuration files, log files etc. are located in files 
 * relative to a directory pointed by zorka.home.dir property:  
 * 
 * ${zorka.home.dir}/conf  - config (.bsh) scripts;
 * ${zorka.home.dir}/log   - log files;
 * 
 * @author rafal.lewczuk@jitlogic.com
 *
 */
public class ZorkaConfig {

    /** Logger */
    private static final ZorkaLog log = ZorkaLogger.getLog(ZorkaConfig.class);

    /** Configuration properties */
    private Properties properties;

    /** Home directory */
	private String homeDir;

    private List<String> profiles = new ArrayList<String>();
    private List<String> profileScripts = new ArrayList<String>();

    /** Path to config defaults (always in classpath) */
    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/core/zorka.properties";

    public static final String PROP_HOME_DIR = "zorka.home.dir";
    public static final String PROP_SCRIPTS_DIR  = "zorka.scripts.dir";;
    public static final String PROP_PROFILE_DIR = "zorka.profile.dir";

    public ZorkaConfig(String home) {
        loadProperties(home);
        loadProfiles();
    }

    public ZorkaConfig(Properties props) {
        properties = props;
        homeDir = props.getProperty(PROP_HOME_DIR);
        setBaseProps();
        loadProfiles();
    }


    /**
     * Returns configuration properties.
     *
     * @return configuration properties
     */
    public Properties getProperties() {
        return properties;
    }


    public String getHomeDir() {
        return homeDir;
    }

    /**
     * Returns path to log directory.
     *
     * @return directory where agent will write its logs.
     */
    public String getLogDir() {
        return ZorkaUtil.path(homeDir, "log");
    }


    public List<String> getProfileScripts() {
        return Collections.unmodifiableList(profileScripts);
    }


    /**
     * Returns default configuration properties. This is read from property file embedded
     * in agent jar file.
     *
     * @return default configuration properties.
     */
    public static Properties defaultProperties() {
        Properties props = new Properties();

        InputStream is = null;

        try {
            is = ZorkaConfig.class.getResourceAsStream(DEFAULT_CONF_PATH);
            props.load(is);
            is.close();
        } catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error loading property file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error closing property file", e);
                }
            }
        }

        return props;
    }



    /**
     * Sets agent home directory and load zorka.properties file from it.
     *
     * @param home home directory for zorka agent
     */
	private void loadProperties(String home) {
        homeDir = home;
		properties = defaultProperties();
        String propPath = ZorkaUtil.path(homeDir, "zorka.properties");
        loadCfg(properties, propPath, true);

        properties.put(PROP_HOME_DIR, homeDir);

        setBaseProps();
	}

    private void setBaseProps() {
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
            File f = new File(ZorkaUtil.path(stringCfg(PROP_PROFILE_DIR, "/"), profile+".profile"));
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


    public Properties loadCfg(Properties properties, String propPath, boolean verbose) {
        InputStream is = null;
        try {
            is = new FileInputStream(propPath);
            properties.load(is);
            return properties;
        } catch (IOException e) {
            if (verbose) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error loading property file", e);
            }
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    if (verbose) {
                        log.error(ZorkaLogger.ZAG_ERRORS, "Error closing property file", e);
                    }
                }
        }

        return null;
    }


    /**
     * Formats string containing references to zorka properties.
     *
     * @param input zorka properties
     *
     * @return
     */
    public String formatCfg(String input) {
        return properties == null ? input : ObjectInspector.substitute(input, properties);
    }


    public List<String> listCfg(String key, String...defVals) {
        return listCfg(properties, key, defVals);
    }


    private List<String> listCfg(Properties properties, String key, String...defVals) {
        String s = properties.getProperty(key);

        if (s != null) {
            String[] ss = s.split(",");
            List<String> lst = new ArrayList<String>(ss.length);
            for (String str : ss) {
                str = str.trim();
                if (str.length() > 0) {
                    lst.add(str);
                }
            }
            return lst;
        } else {
            return Arrays.asList(defVals);
        }
    }


    private static Map<String,Long> kilos = ZorkaUtil.map(
            "k", 1024L, "K", 1024L,
            "m", 1024 * 1024L, "M", 1024 * 1024L,
            "g", 1024 * 1024 * 1024L, "G", 1024 * 1024 * 1024L,
            "t", 1024 * 1024 * 1024 * 1024L, "T", 1024 * 1024 * 1024 * 1024L);


    private static Pattern kiloRe = Pattern.compile("^([0-9]+)([kKmMgGtT])$");


    public Long kiloCfg(String key, Long defval) {
        String s = properties.getProperty(key);

        long multi = 1L;

        if (s != null) {
            Matcher matcher = kiloRe.matcher(s);

            if (matcher.matches()) {
                s = matcher.group(1);
                multi = kilos.get(matcher.group(2));
            }
        }

        try {
            if (s != null) {
                return Long.parseLong(s.trim()) * multi;
            } else {
                return defval;
            }
        } catch (NumberFormatException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return defval;
        }
    }


    public String stringCfg(String key, String defval) {
        String s = properties.getProperty(key);

        return s != null ? s.trim() : defval;
    }


    public Long longCfg(String key, Long defval) {
        String s = properties.getProperty(key);

        try {
            if (s != null) {
                return Long.parseLong(s.trim());
            } else {
                return defval;
            }
        } catch (NumberFormatException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return defval;
        }
    }


    public Integer intCfg(String key, Integer defval) {
        String s = properties.getProperty(key);

        try {
            if (s != null) {
                return Integer.parseInt(s.trim());
            } else {
                return defval;
            }
        } catch (NumberFormatException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            return defval;
        }
    }


    public Boolean boolCfg(String key, Boolean defval) {
        String s = properties.getProperty(key);

        if (s != null) {
            s = s.trim();

            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase(("yes"))) {
                return true;
            } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no")) {
                return false;
            } else {
                log.error(ZorkaLogger.ZAG_ERRORS, "Invalid value for '" + key + "' -> '" + s + "'. Setting default value of '" + defval);
                AgentDiagnostics.inc(AgentDiagnostics.CONFIG_ERRORS);
            }
        }

        return defval;
    }


    public boolean hasCfg(String key) {
        String s = properties.getProperty(key);

        return s != null && s.trim().length() > 0;
    }


    public void setCfg(String key, Object val) {
        properties.setProperty(key, ""+val);
    }

}
