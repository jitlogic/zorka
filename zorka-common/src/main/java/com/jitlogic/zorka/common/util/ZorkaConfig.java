/*
 * Copyright 2012-2017 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.util;

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
    protected Properties properties;

    /** Home directory */
	protected String homeDir;

	private Map<String,String> sysenv = System.getenv();
	private Properties sysprops = System.getProperties();

    public static final String PROP_HOME_DIR = "zorka.home.dir";

    public ZorkaConfig() {
        this(new Properties());
    }

    public ZorkaConfig(Properties properties) {
        this.properties = properties;
    }

    public String get(String key) {
        if (key.startsWith("$")) {
            return sysenv.get(key.substring(1));
        }
        if (key.startsWith("@")) {
            return sysprops.getProperty(key.substring(1));
        }
        if (key.startsWith("&")) {
            return get(key.substring(1));
        }
        String val = properties.getProperty(key);
        return val != null ? ObjectInspector.substitute(
            ObjectInspector.reCfgSubstPattern, val, this) : null;
    }


    public String get(String key, String defval) {
        String rslt = get(key);
        return rslt != null ? rslt : defval;
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



    /**
     * Returns default configuration properties. This is read from property file embedded
     * in agent jar file.
     *
     * @return default configuration properties.
     */
    public static Properties defaultProperties(String path) {
        Properties props = new Properties();

        InputStream is = null;

        try {
            is = ZorkaConfig.class.getResourceAsStream(path);
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
        return properties == null ? input : ObjectInspector.substitute(ObjectInspector.reVarSubstPattern, input, this);
    }


    public List<String> listCfg(String key, String...defVals) {
        String s = get(key);

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

    public Map<String,String> mapCfg(String key, Map<String,String> defVals) {
        Map<String,String> rslt = new HashMap<String, String>();
        rslt.putAll(defVals);

        String prefix = key + ".";

        for (String k : getProperties().stringPropertyNames()) {
            if (k.startsWith(prefix) && k.length() > prefix.length()) {
                String v = stringCfg(k, null);
                if (v != null) {
                    rslt.put(k.substring(prefix.length()), v);
                }
            }
        }

        return rslt;
    }

    public Map<String,String> mapCfg(String key, String... defVals) {
        Map<String,String> dv = new HashMap<String, String>();
        for (int i = 1; i < defVals.length; i+=2) {
            dv.put(defVals[i-1],defVals[i]);
        }
        return mapCfg(key, dv);
    }


    private static Map<String,Long> kilos = ZorkaUtil.map(
            "k", 1024L, "K", 1024L,
            "m", 1024 * 1024L, "M", 1024 * 1024L,
            "g", 1024 * 1024 * 1024L, "G", 1024 * 1024 * 1024L,
            "t", 1024 * 1024 * 1024 * 1024L, "T", 1024 * 1024 * 1024 * 1024L);


    private static Pattern kiloRe = Pattern.compile("^([0-9]+)([kKmMgGtT])$");


    public Long kiloCfg(String key, Long defval) {
        String s = get(key);

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
            markError("Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            return defval;
        }
    }


    public String stringCfg(String key, String defval) {
        String s = get(key);

        return s != null ? s.trim() : defval;
    }


    public Long longCfg(String key, Long defval) {
        String s = get(key);

        try {
            if (s != null) {
                return Long.parseLong(s.trim());
            } else {
                return defval;
            }
        } catch (NumberFormatException e) {
            markError("Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            return defval;
        }
    }


    public Integer intCfg(String key, Integer defval) {
        String s = get(key);

        try {
            if (s != null) {
                return Integer.parseInt(s.trim());
            } else {
                return defval;
            }
        } catch (NumberFormatException e) {
            markError("Cannot parse key '" + key + "' -> '" + s + "'. Returning default value of " + defval + ".", e);
            return defval;
        }
    }


    public Boolean boolCfg(String key, Boolean defval) {
        String s = get(key);

        if (s != null) {
            s = s.trim();

            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase(("yes"))) {
                return true;
            } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no")) {
                return false;
            } else {
                markError("Invalid value for '" + key + "' -> '" + s + "'. Setting default value of '" + defval, null);
            }
        }

        return defval;
    }

    protected void markError(String msg, Throwable e) {
        log.error(ZorkaLogger.ZAG_CONFIG, msg, e);
    }

    public boolean hasCfg(String key) {
        String s = get(key);

        return s != null && s.trim().length() > 0;
    }


    public void setCfg(String key, Object val) {
        properties.setProperty(key, ""+val);
    }

    /**
     * Sets agent home directory and load zorka.properties file from it.
     *
     * @param home home directory for zorka agent
     */
    protected void loadProperties(String home, String fname, String defPath) {
        homeDir = home;
        properties = defaultProperties(defPath);
        String propPath = ZorkaUtil.path(homeDir, fname);
        loadCfg(properties, propPath, true);

        properties.put(PROP_HOME_DIR, homeDir);

    }

}
