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

package com.jitlogic.zorka.agent;

import com.jitlogic.zorka.common.ObjectInspector;
import com.jitlogic.zorka.common.ZorkaLog;
import com.jitlogic.zorka.common.ZorkaLogger;
import com.jitlogic.zorka.common.ZorkaUtil;

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
    private static Properties properties;

    /** Home directory */
	private static String homeDir;

    /** Path to config defaults (always in classpath) */
    public final static String DEFAULT_CONF_PATH = "/com/jitlogic/zorka/agent/zorka.properties";

    /**
     * Clears static agent configuration. This is mainly useful for tests,
     * but it might be also useful for online agent reload feature (which
     * might be implemented some time in the future).
     */
    public static void cleanup() {
        properties = null;
        homeDir = null;
    }


    /**
     * Returns configuration properties.
     *
     * @return configuration properties
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Set configuration properties manually. This is useful mainly for testing.
     *
     * @param props new properties
     */
    public static void setProperties(Properties props) {
        homeDir = props.getProperty("zorka.home.dir", File.separatorChar == '/' ? "/opt/zorka" : "C:\\zorka");
        properties = props;
    }


    /**
     * Returns path to log directory.
     *
     * @return directory where agent will write its logs.
     */
    public static String getLogDir() {
        return ZorkaUtil.path(homeDir, "log");
    }


    /**
     * Returns path to configuration directory.
     *
     * @return directory from which agent reads BSH configuration scripts.
     */
	public static String getConfDir() {
        return ZorkaUtil.path(homeDir, "conf");
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
                is = null;
            }
        }

        return props;
    }

    /**
     * Sets agent home directory and load zorka.properties file from it.
     *
     * @param home home directory for zorka agent
     */
	public static void loadProperties(String home) {
        homeDir = home;
		properties = defaultProperties();
		InputStream is = null;
		try {
            is = new FileInputStream(ZorkaUtil.path(homeDir, "zorka.properties"));
			properties.load(is);
		} catch (IOException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error loading property file", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
                    log.error(ZorkaLogger.ZAG_ERRORS, "Error closing property file", e);
                }
		}

        properties.put("zorka.home.dir", homeDir);
        properties.put("zorka.config.dir", ZorkaUtil.path(homeDir, "conf"));
        properties.put("zorka.log.dir", ZorkaUtil.path(homeDir, "log"));
	}

    /**
     * Formats string containing references to zorka properties.
     *
     * @param input zorka properties
     *
     * @return
     */
    public static String formatCfg(String input) {
        return properties == null ? input : ObjectInspector.substitute(input, properties);
    }

    public static List<String> listCfg(String key, String...defVals) {
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


    public static Long kiloCfg(String key, Long defval) {
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


    public static String stringCfg(String key, String defval) {
        String s = properties.getProperty(key);

        return s != null ? s.trim() : defval;
    }


    public static Long longCfg(String key, Long defval) {
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


    public static Integer intCfg(String key, Integer defval) {
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


    public static Boolean boolCfg(String key, Boolean defval) {
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

    public static boolean hasCfg(String key) {
        String s = properties.getProperty(key);

        return s != null && s.trim().length() > 0;
    }

}
